package net.quiwh.cbjs.content;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;

/** Reduces multiblock face apertures to a bounded set of reusable local panels. */
public final class FluidTankFaceLayout {
    /** u/v are the remaining world axes in X/Y/Z order; edge bits are lowU/highU/lowV/highV. */
    public record Panel(Direction face, Direction.Axis mainAxis, int u0, int v0, int u1, int v1, int edges) {
        public Panel {
            if (face == null || mainAxis == null || u0 < 0 || v0 < 0 || u1 > 16 || v1 > 16
                    || u1 < u0 || v1 < v0 || edges < 0 || edges > 15)
                throw new IllegalArgumentException("Invalid fluid face panel");
        }
        public boolean glass() { return u1 > u0 && v1 > v0; }
        public String key() {
            return mainAxis.getName() + "_" + face.getName() + "_" + u0 + "_" + v0 + "_" + u1 + "_" + v1 + "_" + edges;
        }
    }

    /** Null entries are internal faces. Entries use Direction.get3DDataValue(). */
    public static Panel[] panels(Direction.Axis mainAxis, int width, int length, int x, int y, int z,
            FluidWindowFaces faces) {
        if (width < 1 || length < 1) throw new IllegalArgumentException("Tank dimensions must be positive");
        int[] position = {x, y, z};
        int[] size = {FluidTankGeometry.extent(mainAxis, Direction.Axis.X, width, length),
                FluidTankGeometry.extent(mainAxis, Direction.Axis.Y, width, length),
                FluidTankGeometry.extent(mainAxis, Direction.Axis.Z, width, length)};
        for (int i = 0; i < 3; i++) if (position[i] < 0 || position[i] >= size[i])
            throw new IllegalArgumentException("Tank member outside structure");
        Panel[] result = new Panel[6];
        for (Direction face : Direction.values()) {
            int normal = face.getAxis().ordinal();
            if (position[normal] != (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? size[normal] - 1 : 0)) continue;
            int u = normal == 0 ? 1 : 0, v = normal == 2 ? 1 : 2;
            FluidWindowMode mode = faces.get(face);
            if (mode == FluidWindowMode.NONE) { result[face.get3DDataValue()] = opaque(mainAxis, face); continue; }
            int lowU = 4, highU = size[u] * 16 - 4;
            int lowV = 4, highV = size[v] * 16 - 4;
            if (mode == FluidWindowMode.STRIP) {
                if (u != mainAxis.ordinal()) { lowU = size[u] * 8 - 4; highU = lowU + 8; }
                if (v != mainAxis.ordinal()) { lowV = size[v] * 8 - 4; highV = lowV + 8; }
            }
            int startU = position[u] * 16, startV = position[v] * 16;
            int u0 = Math.max(0, lowU - startU), u1 = Math.min(16, highU - startU);
            int v0 = Math.max(0, lowV - startV), v1 = Math.min(16, highV - startV);
            if (u0 >= u1 || v0 >= v1) { result[face.get3DDataValue()] = opaque(mainAxis, face); continue; }
            int edges = (startU + u0 == lowU ? 1 : 0) | (startU + u1 == highU ? 2 : 0)
                    | (startV + v0 == lowV ? 4 : 0) | (startV + v1 == highV ? 8 : 0);
            result[face.get3DDataValue()] = new Panel(face, mainAxis, u0, v0, u1, v1, edges);
        }
        return result;
    }

    public static Panel opaque(Direction.Axis mainAxis, Direction face) { return new Panel(face, mainAxis, 0, 0, 0, 0, 0); }

    /** Larger structures repeat these same local bounds; asset count never follows multiblock size. */
    public static List<Panel> templates() { return Templates.VALUE; }

    private static final class Templates {
        private static final List<Panel> VALUE = buildTemplates();
    }

    private static List<Panel> buildTemplates() {
        Set<Panel> result = new LinkedHashSet<>();
        for (Direction.Axis axis : Direction.Axis.values()) for (int width = 1; width <= 4; width++)
            for (int length = 1; length <= 3; length++) for (FluidWindowMode mode : FluidWindowMode.values()) {
                int sx = FluidTankGeometry.extent(axis, Direction.Axis.X, width, length);
                int sy = FluidTankGeometry.extent(axis, Direction.Axis.Y, width, length);
                int sz = FluidTankGeometry.extent(axis, Direction.Axis.Z, width, length);
                for (int x = 0; x < sx; x++) for (int y = 0; y < sy; y++) for (int z = 0; z < sz; z++)
                    for (Panel panel : panels(axis, width, length, x, y, z, FluidWindowFaces.filled(mode)))
                        if (panel != null) result.add(panel);
            }
        return List.copyOf(result);
    }

    private FluidTankFaceLayout() { }
}
