package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.drives.DriveCogwheelBlockEntity;
import net.quiwh.cbjs.drives.DriveKind;
import net.quiwh.cbjs.drives.DriveLoop;
import net.quiwh.cbjs.drives.DriveNode;
import net.quiwh.cbjs.drives.DrivePath;

/** Drive strips render independently of the rotating wheel's Flywheel backend. */
public final class StandaloneDriveRenderer {
    private static final int MAX_LOOPS = 256;
    private static final int MAX_FLOATS = 4_194_304;
    private static final Map<UUID, CachedMesh> MESHES = new LinkedHashMap<>(16, .75f, true);
    private static int cachedFloats;
    private static final class CachedMesh {
        final Direction.Axis axis;
        final List<DriveNode> nodes;
        final DriveKind kind;
        final Mesh[] sections;
        double lastTime = Double.NaN;
        double scroll;
        CachedMesh(Direction.Axis axis, List<DriveNode> nodes, DriveKind kind, Mesh[] sections) {
            this.axis = axis; this.nodes = nodes; this.kind = kind; this.sections = sections;
        }
    }
    private record Mesh(float[] vertices) { }

    public static void render(DriveCogwheelBlockEntity entity, float partialTick, PoseStack pose,
                              MultiBufferSource buffers, int light) {
        DriveLoop loop = entity.drive();
        if (loop == null || entity.getLevel() == null) return;
        int index = -1;
        for (int i = 0; i < loop.nodes().size(); i++)
            if (loop.nodes().get(i).pos().equals(entity.getBlockPos())) { index = i; break; }
        if (index < 0) return;
        CachedMesh cached = cache(loop);
        Mesh mesh = cached.sections[index];
        if (mesh == null) {
            cached.sections[index] = mesh = build(loop.path(), loop.kind(), index);
            cachedFloats += mesh.vertices.length;
            trim();
        }
        float scroll = scroll(entity, loop, cached);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(loop.kind().texture(loop.material())));
        var matrix = pose.last();
        for (int i = 0; i < mesh.vertices.length; i += 8) {
            float[] vertex = mesh.vertices;
            consumer.addVertex(matrix, vertex[i], vertex[i + 1], vertex[i + 2])
                    .setColor(-1).setUv(vertex[i + 3], vertex[i + 4] - scroll)
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(matrix, vertex[i + 5], vertex[i + 6], vertex[i + 7]);
        }
    }

    private static float scroll(DriveCogwheelBlockEntity entity, DriveLoop loop, CachedMesh cached) {
        double time = AnimationTickHolder.getRenderTime(entity.getLevel());
        if (Double.isFinite(cached.lastTime) && time >= cached.lastTime && time - cached.lastTime <= 20
                && entity.driveActive()) {
            // Visible track radius includes tooth clearance; native small/large ratio stays 1:2.
            double radius = entity.getBlockState().getBlock() instanceof MaterialGear gear && gear.isLargeCog() ? 1 : .5;
            cached.scroll = (cached.scroll + (time - cached.lastTime) * entity.getSpeed() * radius * Math.PI / 600
                    * tilesPerBlock(loop.path()) * loop.path().winding()) % 1;
        }
        cached.lastTime = time;
        return (float) cached.scroll;
    }

    private static CachedMesh cache(DriveLoop loop) {
        CachedMesh cached = MESHES.get(loop.id());
        if (cached != null && cached.kind == loop.kind() && cached.axis == loop.axis()
                && cached.nodes.equals(loop.nodes())) return cached;
        if (cached != null) cachedFloats -= size(cached);
        cached = new CachedMesh(loop.axis(), loop.nodes(), loop.kind(), new Mesh[loop.nodes().size()]);
        MESHES.put(loop.id(), cached);
        trim();
        return cached;
    }

    private static int size(CachedMesh mesh) {
        int size = 0;
        for (Mesh section : mesh.sections) if (section != null) size += section.vertices.length;
        return size;
    }

    private static void trim() {
        var oldest = MESHES.entrySet().iterator();
        while ((MESHES.size() > MAX_LOOPS || cachedFloats > MAX_FLOATS) && oldest.hasNext()) {
            cachedFloats -= size(oldest.next().getValue());
            oldest.remove();
        }
    }

    public static void clear() { MESHES.clear(); cachedFloats = 0; }

    private static double tilesPerBlock(DrivePath path) {
        // An integer number of repeats closes the material pattern without a visible seam.
        return Math.max(1, Math.round(path.length() * 2)) / path.length();
    }

    private static Mesh build(DrivePath path, DriveKind kind, int index) {
        List<Vec3> points = path.section(index);
        Vec3 origin = Vec3.atLowerCornerOf(path.nodes().get(index).pos());
        Vec3 normal = switch (path.axis()) {
            case X -> new Vec3(1, 0, 0);
            case Y -> new Vec3(0, 1, 0);
            case Z -> new Vec3(0, 0, 1);
        };
        double halfWidth = kind == DriveKind.BELT ? .09375 : kind == DriveKind.CHAIN ? .09375 : .0625;
        double halfHeight = kind == DriveKind.BELT ? .03125 : halfWidth;
        int faces = kind == DriveKind.CHAIN ? 2 : 4;
        float[] data = new float[(points.size() - 1) * faces * 4 * 8];
        Vec3[] radial = new Vec3[points.size()];
        double[] distance = new double[points.size()];
        for (int section = 0; section < index; section++) {
            List<Vec3> previous = path.section(section);
            for (int i = 1; i < previous.size(); i++) distance[0] += previous.get(i).distanceTo(previous.get(i - 1));
        }
        int count = path.nodes().size();
        List<Vec3> previousSection = path.section((index + count - 1) % count);
        List<Vec3> nextSection = path.section((index + 1) % count);
        for (int i = 0; i < points.size(); i++) {
            Vec3 previous = i > 0 ? points.get(i - 1) : previousSection.get(previousSection.size() - 2);
            Vec3 next = i + 1 < points.size() ? points.get(i + 1) : nextSection.get(1);
            radial[i] = normal.cross(next.subtract(previous)).normalize();
            if (i > 0) distance[i] = distance[i - 1] + points.get(i).distanceTo(points.get(i - 1));
        }
        int cursor = 0;
        double tilesPerBlock = tilesPerBlock(path);
        for (int i = 0; i < points.size() - 1; i++) {
            Vec3 a = points.get(i).subtract(origin), b = points.get(i + 1).subtract(origin);
            Vec3 radialA = radial[i], radialB = radial[i + 1];
            float v0 = (float) (distance[i] * tilesPerBlock), v1 = (float) (distance[i + 1] * tilesPerBlock);
            if (kind == DriveKind.CHAIN) {
                cursor = quad(data, cursor, a.add(radialA.scale(-halfHeight)), a.add(radialA.scale(halfHeight)),
                        b.add(radialB.scale(halfHeight)), b.add(radialB.scale(-halfHeight)), normal.scale(-1), normal.scale(-1), v0, v1);
                cursor = quad(data, cursor, a.add(normal.scale(-halfWidth)), a.add(normal.scale(halfWidth)),
                        b.add(normal.scale(halfWidth)), b.add(normal.scale(-halfWidth)), radialA, radialB, v0, v1);
            } else {
                Vec3[] start = corners(a, normal, radialA, halfWidth, halfHeight);
                Vec3[] end = corners(b, normal, radialB, halfWidth, halfHeight);
                for (int face = 0; face < 4; face++) {
                    int next = (face + 1) % 4;
                    Vec3 n0 = face == 0 ? radialA : face == 1 ? normal : face == 2 ? radialA.scale(-1) : normal.scale(-1);
                    Vec3 n1 = face == 0 ? radialB : face == 1 ? normal : face == 2 ? radialB.scale(-1) : normal.scale(-1);
                    cursor = quad(data, cursor, start[face], start[next], end[next], end[face], n0, n1, v0, v1);
                }
            }
        }
        return new Mesh(data);
    }

    private static Vec3[] corners(Vec3 point, Vec3 normal, Vec3 radial, double width, double height) {
        Vec3 n = normal.scale(width), r = radial.scale(height);
        return new Vec3[]{point.subtract(n).add(r), point.add(n).add(r), point.add(n).subtract(r), point.subtract(n).subtract(r)};
    }

    private static int quad(float[] data, int cursor, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                            Vec3 normalStart, Vec3 normalEnd, float v0, float v1) {
        cursor = vertex(data, cursor, a, 0, v0, normalStart);
        cursor = vertex(data, cursor, b, 1, v0, normalStart);
        cursor = vertex(data, cursor, c, 1, v1, normalEnd);
        return vertex(data, cursor, d, 0, v1, normalEnd);
    }

    private static int vertex(float[] data, int cursor, Vec3 point, float u, float v, Vec3 normal) {
        data[cursor++] = (float) point.x; data[cursor++] = (float) point.y; data[cursor++] = (float) point.z;
        data[cursor++] = u; data[cursor++] = v;
        data[cursor++] = (float) normal.x; data[cursor++] = (float) normal.y; data[cursor++] = (float) normal.z;
        return cursor;
    }

    private StandaloneDriveRenderer() { }
}
