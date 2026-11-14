package net.quiwh.cbjs.drives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Immutable planar outer loop shared by drive rendering, selection and riding. */
public final class DrivePath {
    public static final int MAX_SAMPLES = 4096;
    private static final double EPSILON = 1e-8;
    private static final double STEP = .125;
    private static final double ANGLE_STEP = Math.PI / 18;
    private static final double TWO_PI = Math.PI * 2;

    private final Direction.Axis axis;
    private final int winding;
    private final List<DriveNode> nodes;
    private final List<Vec3> samples;
    private final List<List<Vec3>> sections;
    private final List<AABB> bounds;
    private final double[] lengths;

    private DrivePath(Direction.Axis axis, int winding, List<DriveNode> nodes, List<List<Vec3>> sections) {
        this.axis = axis;
        this.winding = winding;
        this.nodes = List.copyOf(nodes);
        this.sections = List.copyOf(sections);
        var points = new ArrayList<Vec3>();
        var boxes = new ArrayList<AABB>();
        for (List<Vec3> section : sections) {
            AABB box = new AABB(section.getFirst(), section.getFirst());
            for (Vec3 point : section) box = box.minmax(new AABB(point, point));
            boxes.add(box.inflate(.15));
            points.addAll(points.isEmpty() ? section : section.subList(1, section.size()));
        }
        if (points.size() > MAX_SAMPLES)
            throw new IllegalArgumentException("Drive path exceeds the sample budget");
        samples = List.copyOf(points);
        bounds = List.copyOf(boxes);
        lengths = new double[samples.size()];
        for (int i = 1; i < lengths.length; i++)
            lengths[i] = lengths[i - 1] + samples.get(i).distanceTo(samples.get(i - 1));
    }

    public static DrivePath create(Direction.Axis axis, List<DriveNode> input) {
        Objects.requireNonNull(axis, "Drive axis");
        List<DriveNode> nodes = List.copyOf(input);
        int count = nodes.size();
        if (count < 2 || count > 16)
            throw new IllegalArgumentException("A drive loop needs 2 to 16 wheels");
        var positions = new HashSet<>();
        double[] x = new double[count];
        double[] y = new double[count];
        int plane = nodes.getFirst().pos().get(axis);
        double minX = Double.POSITIVE_INFINITY, minY = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX;
        for (int i = 0; i < count; i++) {
            DriveNode node = nodes.get(i);
            if (!positions.add(node.pos()) || node.pos().get(axis) != plane)
                throw new IllegalArgumentException("Drive wheels must be distinct and coplanar");
            Vec3 centre = node.pos().getCenter();
            x[i] = coordinateU(axis, centre);
            y[i] = coordinateV(axis, centre);
            minX = Math.min(minX, x[i]); maxX = Math.max(maxX, x[i]);
            minY = Math.min(minY, y[i]); maxY = Math.max(maxY, y[i]);
        }
        if (maxX - minX > 64 || maxY - minY > 64)
            throw new IllegalArgumentException("Drive span cannot exceed 64 blocks");
        for (int i = 0; i < count; i++) for (int j = i + 1; j < count; j++)
            if (Math.hypot(x[i] - x[j], y[i] - y[j]) <= nodes.get(i).radius() + nodes.get(j).radius() + EPSILON)
                throw new IllegalArgumentException("Drive wheel tracks overlap");

        int winding = 1;
        if (count > 2) {
            double area = 0;
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                area += (x[i] - x[0]) * (y[next] - y[0]) - (y[i] - y[0]) * (x[next] - x[0]);
            }
            if (Math.abs(area) < EPSILON)
                throw new IllegalArgumentException("Drive loop has no enclosed area");
            winding = area > 0 ? 1 : -1;
            // Every other centre must lie strictly inside every directed edge.
            // This rejects concavity, collinear redundant nodes and star polygons.
            for (int i = 0; i < count; i++) {
                int next = (i + 1) % count;
                for (int j = 0; j < count; j++) {
                    if (j == i || j == next) continue;
                    double cross = (x[next] - x[i]) * (y[j] - y[i]) - (y[next] - y[i]) * (x[j] - x[i]);
                    if (cross * winding <= EPSILON)
                        throw new IllegalArgumentException("Drive nodes must form a simple strictly convex loop");
                }
            }
        }

        double[] nx = new double[count], ny = new double[count];
        for (int i = 0; i < count; i++) {
            int next = (i + 1) % count;
            double dx = x[next] - x[i], dy = y[next] - y[i];
            double distance = Math.hypot(dx, dy);
            double parallel = (nodes.get(i).radius() - nodes.get(next).radius()) / distance;
            double outward = Math.sqrt(1 - parallel * parallel) * winding;
            nx[i] = dx / distance * parallel + dy / distance * outward;
            ny[i] = dy / distance * parallel - dx / distance * outward;
            // Variable-radius tracks must also be exposed on the outside hull.
            for (int j = 0; j < count; j++)
                if (nx[i] * (x[j] - x[i]) + ny[i] * (y[j] - y[i]) + nodes.get(j).radius()
                        > nodes.get(i).radius() + EPSILON)
                    throw new IllegalArgumentException("A wheel is hidden inside the drive track hull");
        }

        var sections = new ArrayList<List<Vec3>>();
        int sampleCount = 1;
        for (int i = 0; i < count; i++) {
            int previous = (i + count - 1) % count, next = (i + 1) % count;
            double radius = nodes.get(i).radius();
            double start = Math.atan2(ny[previous], nx[previous]);
            double sweep = positiveAngle(winding * (Math.atan2(ny[i], nx[i]) - start));
            if (sweep < EPSILON || (count > 2 && sweep >= Math.PI + EPSILON))
                throw new IllegalArgumentException("Drive has an invalid wheel wrap");
            int arcSteps = Math.max(1, (int) Math.ceil(sweep / Math.min(ANGLE_STEP, STEP / radius)));
            Vec3 tangentStart = world(axis, plane + .5, x[i] + nx[i] * radius, y[i] + ny[i] * radius);
            Vec3 tangentEnd = world(axis, plane + .5, x[next] + nx[i] * nodes.get(next).radius(),
                    y[next] + ny[i] * nodes.get(next).radius());
            int lineSteps = Math.max(1, (int) Math.ceil(tangentStart.distanceTo(tangentEnd) / STEP));
            sampleCount += arcSteps + lineSteps;
            if (sampleCount > MAX_SAMPLES)
                throw new IllegalArgumentException("Drive path exceeds the sample budget");
            var section = new ArrayList<Vec3>(arcSteps + lineSteps + 1);
            for (int step = 0; step <= arcSteps; step++) {
                double angle = start + winding * sweep * step / arcSteps;
                section.add(world(axis, plane + .5, x[i] + Math.cos(angle) * radius, y[i] + Math.sin(angle) * radius));
            }
            // Snap shared tangent endpoints to the same arithmetic on both sections.
            section.set(section.size() - 1, tangentStart);
            for (int step = 1; step <= lineSteps; step++)
                section.add(tangentStart.lerp(tangentEnd, (double) step / lineSteps));
            sections.add(List.copyOf(section));
        }
        return new DrivePath(axis, winding, nodes, sections);
    }

    public double length() { return lengths[lengths.length - 1]; }
    public Direction.Axis axis() { return axis; }
    /** Direction of increasing arclength around the positive rotation axis. */
    public int winding() { return winding; }
    public List<DriveNode> nodes() { return nodes; }
    public List<Vec3> samples() { return samples; }
    public List<Vec3> section(int index) { return sections.get(index); }
    public AABB bounds(int index) { return bounds.get(index); }

    public Vec3 point(double distance) {
        double wrapped = wrap(distance);
        int segment = segment(wrapped);
        return samples.get(segment).lerp(samples.get(segment + 1),
                (wrapped - lengths[segment]) / (lengths[segment + 1] - lengths[segment]));
    }

    public Vec3 tangent(double distance) {
        int segment = segment(wrap(distance));
        return samples.get(segment + 1).subtract(samples.get(segment)).normalize();
    }

    /** Returns arclength of the closest point on the closed sampled track. */
    public double nearestDistance(Vec3 point) {
        requireFinite(point);
        double nearestSquared = Double.POSITIVE_INFINITY, result = 0;
        for (int i = 0; i < samples.size() - 1; i++) {
            Vec3 start = samples.get(i), delta = samples.get(i + 1).subtract(start);
            double fraction = clamp(point.subtract(start).dot(delta) / delta.lengthSqr(), 0, 1);
            double squared = point.distanceToSqr(start.add(delta.scale(fraction)));
            if (squared < nearestSquared) {
                nearestSquared = squared;
                result = lengths[i] + fraction * (lengths[i + 1] - lengths[i]);
            }
        }
        return result;
    }

    /** Returns track arclength hit first by the finite ray, or -1 when it misses. */
    public double rayDistance(Vec3 origin, Vec3 direction, double reach, double tolerance) {
        requireFinite(origin); requireFinite(direction);
        if (!Double.isFinite(reach) || !Double.isFinite(tolerance) || reach < 0 || tolerance < 0
                || direction.lengthSqr() < EPSILON) return -1;
        Vec3 ray = direction.normalize();
        double firstRayDistance = Double.POSITIVE_INFINITY, result = -1;
        for (int i = 0; i < samples.size() - 1; i++) {
            Vec3 start = samples.get(i), edge = samples.get(i + 1).subtract(start), offset = origin.subtract(start);
            double b = ray.dot(edge), c = edge.lengthSqr(), d = ray.dot(offset), e = edge.dot(offset);
            double denominator = c - b * b;
            double t = denominator > EPSILON ? clamp((b * e - c * d) / denominator, 0, reach) : 0;
            double u = clamp((e + b * t) / c, 0, 1);
            t = clamp(b * u - d, 0, reach);
            u = clamp((e + b * t) / c, 0, 1);
            if (origin.add(ray.scale(t)).distanceToSqr(start.add(edge.scale(u))) <= tolerance * tolerance
                    && t < firstRayDistance) {
                firstRayDistance = t;
                result = lengths[i] + u * (lengths[i + 1] - lengths[i]);
            }
        }
        return result;
    }

    private int segment(double wrapped) {
        int index = Arrays.binarySearch(lengths, wrapped);
        return index < 0 ? -index - 2 : Math.min(index, lengths.length - 2);
    }
    private double wrap(double distance) {
        if (!Double.isFinite(distance)) throw new IllegalArgumentException("Drive distance must be finite");
        double wrapped = distance % length();
        return wrapped < 0 ? wrapped + length() : wrapped;
    }
    private static double positiveAngle(double angle) {
        double result = angle % TWO_PI;
        return result < 0 ? result + TWO_PI : result;
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    private static void requireFinite(Vec3 point) {
        if (point == null || !Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z))
            throw new IllegalArgumentException("Drive point must be finite");
    }
    private static double coordinateU(Direction.Axis axis, Vec3 point) {
        return switch (axis) { case X -> point.y; case Y -> point.z; case Z -> point.x; };
    }
    private static double coordinateV(Direction.Axis axis, Vec3 point) {
        return switch (axis) { case X -> point.z; case Y -> point.x; case Z -> point.y; };
    }
    private static Vec3 world(Direction.Axis axis, double plane, double u, double v) {
        return switch (axis) { case X -> new Vec3(plane, u, v); case Y -> new Vec3(v, plane, u); case Z -> new Vec3(u, v, plane); };
    }
}
