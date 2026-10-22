package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.FluidTankFaceLayout;
import net.quiwh.cbjs.content.FluidTankFaceLayout.Panel;

/** Independent six-face hull fragments, with native tank artwork and seamless pane sampling. */
public final class FluidTankFaceAssets {
    public static ResourceLocation modelLocation(ResourceLocation blockId, Panel panel) {
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath() + "/faces/" + panel.key());
    }

    static void generate(FluidTankBuilder builder, AssetJsonGenerator generator) {
        for (Panel panel : FluidTankFaceLayout.templates()) {
            if (builder.isVessel() == (panel.mainAxis() == Direction.Axis.Y)) continue;
            GearAssets.put(generator, builder.id, "models", modelLocation(builder.id, panel).getPath(), model(builder, panel));
        }
        Direction.Axis axis = builder.isVessel() ? Direction.Axis.X : Direction.Axis.Y;
        JsonObject single = base(builder);
        JsonArray elements = new JsonArray();
        for (Panel panel : FluidTankFaceLayout.panels(axis, 1, 1, 0, 0, 0, builder.defaultWindowFaces(axis)))
            elements.addAll(model(builder, panel).getAsJsonArray("elements"));
        single.add("elements", elements);
        if (builder.isVessel()) {
            JsonObject gui = new JsonObject(); gui.add("rotation", numbers(30, 135, 0));
            gui.add("scale", numbers(.625, .625, .625));
            JsonObject display = new JsonObject(); display.add("gui", gui); single.add("display", display);
        }
        GearAssets.put(generator, builder.id, "models", "block/" + builder.id.getPath() + "/single", single);
    }

    public static JsonObject model(FluidTankBuilder builder, Panel panel) {
        JsonObject model = base(builder);
        JsonArray elements = new JsonArray();
        if (!panel.glass()) metal(elements, panel, 0, 16, 0, 16);
        else {
            if (panel.u0() > 0) metal(elements, panel, 0, panel.u0(), 0, 16);
            if (panel.u1() < 16) metal(elements, panel, panel.u1(), 16, 0, 16);
            if (panel.v0() > 0) metal(elements, panel, panel.u0(), panel.u1(), 0, panel.v0());
            if (panel.v1() < 16) metal(elements, panel, panel.u0(), panel.u1(), panel.v1(), 16);
            boolean longPane = longPane(panel);
            int normal = panel.face().getAxis().ordinal();
            int uAxis = normal == 0 ? 1 : 0;
            double uSpan = longPane && panel.mainAxis().ordinal() == uAxis ? 16 : 8;
            double vSpan = longPane && panel.mainAxis().ordinal() != uAxis ? 16 : 8;
            for (Slice u : slices(panel.u0(), panel.u1(), (panel.edges() & 1) != 0, (panel.edges() & 2) != 0, uSpan))
                for (Slice v : slices(panel.v0(), panel.v1(), (panel.edges() & 4) != 0, (panel.edges() & 8) != 0, vSpan))
                    glass(elements, panel, u, v, uSpan, vSpan, longPane);
        }
        model.add("elements", elements);
        return model;
    }

    private static JsonObject base(FluidTankBuilder builder) {
        JsonObject model = GearAssets.parent("minecraft:block/block");
        // Inward faces share their outer surface's cull group; there is no occlusion at member joins.
        model.addProperty("ambientocclusion", false);
        JsonObject textures = new JsonObject();
        for (var entry : Map.of("0", "fluid_tank_top", "1", "fluid_tank", "3", "fluid_tank_window",
                "4", "fluid_tank_inner", "5", "fluid_tank_window_single", "particle", "fluid_tank").entrySet())
            textures.addProperty(entry.getKey(), builder.spritePath(entry.getValue()));
        model.add("textures", textures);
        return model;
    }

    private record Slice(double from, double to, double uvFrom, double uvTo) { }

    /** Preserve the native two-pixel rounded frame only at the entire aperture's exposed perimeter. */
    private static List<Slice> slices(double from, double to, boolean low, boolean high, double span) {
        if (low && high && to - from == span) return List.of(new Slice(from, to, 0, span));
        List<Slice> result = new ArrayList<>(3);
        double start = low ? from + 2 : from, end = high ? to - 2 : to;
        if (low) result.add(new Slice(from, start, 0, 2));
        if (end > start) result.add(new Slice(start, end, 2, span - 2));
        if (high) result.add(new Slice(end, to, span - 2, span));
        return result;
    }

    private static boolean longPane(Panel panel) {
        if (!panel.face().getAxis().isHorizontal() || panel.face().getAxis() == panel.mainAxis()) return false;
        int uAxis = panel.face().getAxis() == Direction.Axis.X ? 1 : 0;
        boolean alongU = panel.mainAxis().ordinal() == uAxis;
        return alongU ? (panel.edges() & 3) != 3 || panel.u1() - panel.u0() != 8
                : (panel.edges() & 12) != 12 || panel.v1() - panel.v0() != 8;
    }

    private static void glass(JsonArray elements, Panel panel, Slice u, Slice v,
            double uSpan, double vSpan, boolean longPane) {
        Direction outer = panel.face();
        double surface = outer.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 15.05 : .95;
        double[] from = point(outer, u.from, v.from, surface), to = point(outer, u.to, v.to, surface);
        JsonObject faces = new JsonObject();
        for (Direction normal : new Direction[]{outer, outer.getOpposite()}) {
            double[] uv;
            int rotation = 0;
            if (panel.mainAxis() == Direction.Axis.X && normal.getAxis() == Direction.Axis.Y) {
                // The Z vessel is the source frame; rotate roof and floor with the X vessel.
                uv = normal == Direction.UP
                        ? new double[]{vSpan - v.uvTo, u.uvFrom, vSpan - v.uvFrom, u.uvTo}
                        : new double[]{vSpan - v.uvTo, uSpan - u.uvTo, vSpan - v.uvFrom, uSpan - u.uvFrom};
                rotation = normal == Direction.UP ? 270 : 90;
            } else if (panel.mainAxis().isHorizontal() && normal.getAxis().isHorizontal() && normal.getAxis() != panel.mainAxis()) {
                Slice y = normal.getAxis() == Direction.Axis.X ? u : v;
                Slice length = normal.getAxis() == Direction.Axis.X ? v : u;
                boolean upward = normal == Direction.NORTH || normal == Direction.EAST;
                double longitudinalSpan = longPane ? 16 : 8;
                uv = upward ? new double[]{y.uvFrom, longitudinalSpan - length.uvTo, y.uvTo, longitudinalSpan - length.uvFrom}
                        : new double[]{8 - y.uvTo, longitudinalSpan - length.uvTo, 8 - y.uvFrom, longitudinalSpan - length.uvFrom};
                rotation = upward ? 270 : 90;
            } else uv = switch (normal) {
                case DOWN -> new double[]{u.uvFrom, vSpan - v.uvTo, u.uvTo, vSpan - v.uvFrom};
                case UP -> new double[]{u.uvFrom, v.uvFrom, u.uvTo, v.uvTo};
                case NORTH -> new double[]{uSpan - u.uvTo, vSpan - v.uvTo, uSpan - u.uvFrom, vSpan - v.uvFrom};
                case SOUTH -> new double[]{u.uvFrom, vSpan - v.uvTo, u.uvTo, vSpan - v.uvFrom};
                case WEST -> new double[]{v.uvFrom, uSpan - u.uvTo, v.uvTo, uSpan - u.uvFrom};
                case EAST -> new double[]{vSpan - v.uvTo, uSpan - u.uvTo, vSpan - v.uvFrom, uSpan - u.uvFrom};
            };
            if (panel.mainAxis().isHorizontal() && normal.getAxis() != panel.mainAxis()) {
                double sourceHeight = normal.getAxis() == Direction.Axis.Y
                        ? panel.mainAxis() == Direction.Axis.X ? uSpan : vSpan : longPane ? 16 : 8;
                flipV(uv, sourceHeight);
            }
            JsonObject face = new JsonObject(); face.add("uv", numbers(uv));
            if (rotation != 0) face.addProperty("rotation", rotation);
            face.addProperty("texture", longPane ? "#3" : "#5"); face.addProperty("cullface", outer.getName());
            faces.add(normal.getName(), face);
        }
        elements.add(element(from, to, faces));
    }

    private static void metal(JsonArray elements, Panel panel, double u0, double u1, double v0, double v1) {
        Direction outer = panel.face();
        boolean positive = outer.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double thickness = outer.getAxis() == panel.mainAxis() ? 4 : 1;
        double[] from = point(outer, u0, v0, positive ? 16 - thickness : 0);
        double[] to = point(outer, u1, v1, positive ? 16 : thickness);
        JsonObject faces = new JsonObject();
        for (Direction normal : Direction.values()) {
            double[] faceFrom = from, faceTo = to;
            if (normal.getAxis() != outer.getAxis()) {
                int dimension = normal.getAxis().ordinal();
                double boundary = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to[dimension] : from[dimension];
                if (boundary == 0 || boundary == 16) continue;
                // The four-pixel end cap already owns this plane, including its outer one-pixel rim.
                // Drawing the side-window reveal here repeats that surface with a different sprite.
                if (normal.getAxis() == panel.mainAxis() && (boundary == 4 || boundary == 12)) continue;
                int uAxis = outer.getAxis() == Direction.Axis.X ? 1 : 0;
                boolean alongU = dimension == uAxis;
                int other = alongU ? outer.getAxis() == Direction.Axis.Z ? 1 : 2 : uAxis;
                boolean towardPositive = normal.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                double apertureEdge = alongU ? towardPositive ? panel.u0() : panel.u1()
                        : towardPositive ? panel.v0() : panel.v1();
                if (!panel.glass() || boundary != apertureEdge) continue;
                // A frame rectangle's ends are buried against its neighbors. Only expose the actual opening.
                faceFrom = from.clone(); faceTo = to.clone();
                faceFrom[dimension] = faceTo[dimension] = boundary;
                faceFrom[other] = Math.max(from[other], alongU ? panel.v0() : panel.u0());
                faceTo[other] = Math.min(to[other], alongU ? panel.v1() : panel.u1());
                if (faceFrom[other] >= faceTo[other]) continue;
            }
            String role = normal == outer ? outer.getAxis() == panel.mainAxis() ? "#0" : "#1"
                    : normal == outer.getOpposite() && outer.getAxis() == panel.mainAxis() ? "#4" : "#1";
            JsonObject face = metalFace(panel.mainAxis(), faceFrom, faceTo, normal, role);
            face.addProperty("cullface", outer.getName());
            if (normal.getAxis() == outer.getAxis()) faces.add(normal.getName(), face);
            else {
                JsonObject reveal = new JsonObject(); reveal.add(normal.getName(), face);
                elements.add(element(faceFrom, faceTo, reveal));
            }
        }
        elements.add(element(from, to, faces));
    }

    private static JsonObject metalFace(Direction.Axis main, double[] from, double[] to, Direction normal, String role) {
        double[] uv = switch (normal) {
            case DOWN -> new double[]{from[0], 16 - to[2], to[0], 16 - from[2]};
            case UP -> new double[]{from[0], from[2], to[0], to[2]};
            case NORTH -> new double[]{16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1]};
            case SOUTH -> new double[]{from[0], 16 - to[1], to[0], 16 - from[1]};
            case WEST -> new double[]{from[2], 16 - to[1], to[2], 16 - from[1]};
            case EAST -> new double[]{16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1]};
        };
        JsonObject result = new JsonObject();
        if (main == Direction.Axis.X && normal.getAxis() == Direction.Axis.Y) {
            uv = normal == Direction.UP ? new double[]{16 - to[2], from[0], 16 - from[2], to[0]}
                    : new double[]{16 - to[2], 16 - to[0], 16 - from[2], 16 - from[0]};
            result.addProperty("rotation", normal == Direction.UP ? 270 : 90);
        } else if (main.isHorizontal() && normal.getAxis().isHorizontal() && normal.getAxis() != main && role.equals("#1")) {
            boolean upward = normal == Direction.NORTH || normal == Direction.EAST;
            int l = main.ordinal();
            uv = upward ? new double[]{from[1], 16 - to[l], to[1], 16 - from[l]}
                    : new double[]{16 - to[1], 16 - to[l], 16 - from[1], 16 - from[l]};
            result.addProperty("rotation", upward ? 270 : 90);
        }
        if (main.isHorizontal() && normal.getAxis() != main) flipV(uv, 16);
        result.add("uv", numbers(uv)); result.addProperty("texture", role);
        return result;
    }

    /** Side artwork is quarter-turned, so reflecting source V flips it along the vessel's length. */
    private static void flipV(double[] uv, double span) {
        uv[1] = span - uv[1];
        uv[3] = span - uv[3];
    }

    private static double[] point(Direction normal, double u, double v, double n) {
        return switch (normal.getAxis()) {
            case X -> new double[]{n, u, v};
            case Y -> new double[]{u, n, v};
            case Z -> new double[]{u, v, n};
        };
    }
    private static JsonObject element(double[] from, double[] to, JsonObject faces) {
        JsonObject result = new JsonObject(); result.add("from", numbers(from)); result.add("to", numbers(to));
        result.add("faces", faces); return result;
    }
    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray(); for (double value : values) result.add(value); return result;
    }
    private FluidTankFaceAssets() { }
}
