package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;

/** Casing surfaces, gear windows and connection ports using Create's native texture layouts. */
final class CasingAssets {
    private CasingAssets() { }

    static void generate(CasingVariantBuilder builder, AssetJsonGenerator generator) {
        ResourceLocation id = builder.id;
        String root = "block/" + id.getPath();
        String texture = builder.casing.casingTexture();
        String shaft = builder.casing.shaftTexture();
        String preset = builder.gear == null ? null : builder.gear.importedModel;
        JsonObject item = null;
        switch (builder.kind) {
            case SHAFT -> {
                GearAssets.put(generator, id, "models", root + "/block", shaftModel(texture, shaft, false));
                if (builder.itemBuilder != null) item = builder.shaft == null ? shaftModel(texture, shaft, true)
                        : shaftItem(texture, shaft, builder.shaft);
            }
            case PIPE -> {
                String pipe = builder.casing.textures.has("pipe") ? builder.casing.pipeTexture() : null;
                String material = builder.pipe == null ? "create:block/pipes" : builder.pipe.pipeTexture();
                GearAssets.put(generator, id, "models", root + "/block_flat", pipeModel(texture, pipe, material, false));
                GearAssets.put(generator, id, "models", root + "/block_open", pipeModel(texture, pipe, material, true));
                if (builder.itemBuilder != null) item = pipeItem(texture, pipe, material);
            }
            case COGWHEEL, LARGE_COGWHEEL -> {
                boolean large = builder.kind == CasingVariantBuilder.Kind.LARGE_COGWHEEL;
                String side = large && builder.casing.cogwheelSideConnectedTexture() != null
                        ? builder.casing.cogwheelSideConnectedTexture() : builder.casing.cogwheelSideTexture();
                for (boolean bottom : new boolean[]{false, true}) for (boolean top : new boolean[]{false, true}) {
                    String suffix = GearAssets.suffix(bottom, top);
                    GearAssets.put(generator, id, "models", root + "/block" + suffix,
                            cogwheelModel(preset, large, builder.gear != null && builder.gear.shaftless ? "" : suffix,
                                    texture, side, shaft));
                }
                if (builder.itemBuilder != null)
                    item = builder.gear != null && GearAssets.customWheelModel(builder.gear)
                            ? GearAssets.encasedComposite(builder.gear, large,
                                cogwheelModel(preset, large, "", texture, side, shaft), texture)
                            : cogwheelItem(preset, large, texture, side, shaft,
                                builder.gear == null ? new JsonObject() : builder.gear.textures,
                                builder.gear == null ? null : builder.gear.allTexture);
            }
            default -> throw new IllegalArgumentException("Unsupported casing variant " + builder.kind);
        }
        JsonObject state = builder.kind == CasingVariantBuilder.Kind.PIPE ? pipeState(id)
                : axisState(id, builder.kind == CasingVariantBuilder.Kind.SHAFT);
        GearAssets.put(generator, id, "blockstates", id.getPath(),
                builder.blockstateJson == null ? state : builder.blockstateJson);
        if (builder.itemBuilder != null) {
            item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson, builder.itemBuilder.parentModel,
                    builder.itemBuilder.textureJson);
            GearAssets.put(generator, id, "models/item", id.getPath(), item);
        }
    }

    /** All stationary housings inherit Create's installed model instead of copying its geometry. */
    static JsonObject cogwheelModel(String preset, boolean large, String suffix, String texture) {
        return cogwheelModel(preset, large, suffix, texture, null, "create:block/gearbox");
    }

    static JsonObject cogwheelModel(String preset, boolean large, String suffix, String texture, String side, String shaft) {
        String size = large ? "large_cogwheel" : "cogwheel";
        ResourceLocation source = ResourceLocation.fromNamespaceAndPath("create", "block/encased_" + size + "/block" + suffix);
        JsonObject model = GearAssets.parent(source.toString());
        casingSurfaces(model, texture, side);
        model.getAsJsonObject("textures").addProperty("4", shaft);
        // The native small housing has an alpha belt. A single solid casing texture needs windows.
        if (!large && side == null) {
            JsonObject geometry = ScriptModelResources.resolved(source);
            copyTextures(model, geometry);
            model = geometry;
            openSmallCogwheelBelt(model);
        }
        return model;
    }

    static JsonObject stockCogwheelModel(boolean large, String suffix, boolean brass) {
        return GearAssets.parent("create:block/" + (brass ? "brass" : "andesite") + "_encased_"
                + (large ? "large_cogwheel" : "cogwheel") + suffix);
    }

    static JsonObject cogwheelItem(String preset, boolean large, String texture, JsonObject overrides, String allTexture) {
        return cogwheelItem(preset, large, texture, null, "create:block/gearbox", overrides, allTexture);
    }

    static JsonObject cogwheelItem(String preset, boolean large, String texture, String side, String shaft,
                                   JsonObject overrides, String allTexture) {
        String size = large ? "large_cogwheel" : "cogwheel";
        ResourceLocation item = ResourceLocation.fromNamespaceAndPath("create", "block/encased_" + size + "/item");
        if (preset == null && overrides.entrySet().isEmpty() && allTexture == null && (large || side != null)) {
            JsonObject model = GearAssets.parent(item.toString());
            casingSurfaces(model, texture, side);
            return model;
        }
        // A material item needs the native casing shell and its selected rotating gear in one model.
        // No separate encased item JSON is needed for each material/casing combination.
        JsonObject model = ScriptModelResources.resolved(ResourceLocation.fromNamespaceAndPath("create",
                "block/encased_" + size + "/block"));
        JsonObject itemDisplay = ScriptModelResources.load(item);
        model.addProperty("parent", itemDisplay.has("parent") ? itemDisplay.get("parent").getAsString() : "minecraft:block/block");
        casingSurfaces(model, texture, side);
        if (!large && side == null) openSmallCogwheelBelt(model);
        JsonObject gear = ScriptModelResources.resolved(preset == null
                ? ResourceLocation.fromNamespaceAndPath("create", "block/" + size + "_shaftless")
                : GearAssets.partSource(preset, size + "_shaftless"));
        GearAssets.applyTextures(gear, overrides, false, true, allTexture);
        appendPart(model, gear, "gear_");
        removeCullFaces(model);
        return model;
    }

    static JsonObject materialItem(String preset, boolean large, boolean brass, JsonObject overrides, String allTexture) {
        String casing = brass ? "brass" : "andesite";
        JsonObject model = cogwheelItem(preset, large, "create:block/" + casing + "_casing",
                "create:block/" + casing + "_encased_cogwheel_side" + (large ? "_connected" : ""),
                "create:block/" + (brass ? "brass_gearbox" : "gearbox"), overrides, allTexture);
        model.getAsJsonObject("textures").addProperty("1", "minecraft:block/stripped_"
                + (brass ? "dark_oak" : "spruce") + "_log_top");
        return model;
    }

    private static void casingSurfaces(JsonObject model, String texture, String side) {
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        for (String name : new String[]{"casing", "particle", "1"}) slots.addProperty(name, texture);
        slots.addProperty("side", side == null ? texture : side);
        model.add("textures", slots);
    }

    /** Merge part texture slots under a prefix so numeric material slots cannot replace casing slots. */
    private static void appendPart(JsonObject model, JsonObject part, String prefix) {
        JsonObject target = model.getAsJsonObject("textures");
        if (part.has("textures")) for (var slot : part.getAsJsonObject("textures").entrySet()) {
            String value = slot.getValue().getAsString();
            target.addProperty(prefix + slot.getKey(), value.startsWith("#") ? "#" + prefix + value.substring(1) : value);
        }
        if (!part.has("elements")) throw new IllegalArgumentException("Cogwheel part has no element geometry");
        JsonArray elements = model.getAsJsonArray("elements");
        for (JsonElement value : part.getAsJsonArray("elements")) {
            JsonObject element = value.deepCopy().getAsJsonObject();
            for (var face : element.getAsJsonObject("faces").entrySet()) {
                JsonObject json = face.getValue().getAsJsonObject();
                String texture = json.get("texture").getAsString();
                if (texture.startsWith("#")) json.addProperty("texture", "#" + prefix + texture.substring(1));
            }
            elements.add(element);
        }
        model.remove("groups");
    }

    private static void copyTextures(JsonObject from, JsonObject to) {
        JsonObject target = to.has("textures") ? to.getAsJsonObject("textures") : new JsonObject();
        for (var slot : from.getAsJsonObject("textures").entrySet()) target.add(slot.getKey(), slot.getValue());
        to.add("textures", target);
    }

    private static void removeCullFaces(JsonObject model) {
        for (JsonElement value : model.getAsJsonArray("elements"))
            for (var face : value.getAsJsonObject().getAsJsonObject("faces").entrySet())
                face.getValue().getAsJsonObject().remove("cullface");
    }

    /** The original middle belt relies on an alpha texture; solid textures need geometric windows. */
    private static void openSmallCogwheelBelt(JsonObject model) {
        JsonArray elements = model.getAsJsonArray("elements");
        JsonArray open = new JsonArray();
        int bottom = 16;
        int top = 0;
        for (JsonElement value : elements) {
            JsonObject element = value.getAsJsonObject();
            var from = element.getAsJsonArray("from");
            var to = element.getAsJsonArray("to");
            boolean belt = from.get(1).getAsDouble() < 8 && to.get(1).getAsDouble() > 8
                    && Math.abs(to.get(0).getAsDouble() - from.get(0).getAsDouble()) > 15
                    && Math.abs(to.get(2).getAsDouble() - from.get(2).getAsDouble()) > 15
                    && element.getAsJsonObject("faces").entrySet().stream().allMatch(face ->
                            face.getValue().getAsJsonObject().get("texture").getAsString().equals("#side"));
            if (!belt) open.add(element);
            else {
                bottom = Math.min(bottom, from.get(1).getAsInt());
                top = Math.max(top, to.get(1).getAsInt());
            }
        }
        if (top > bottom) for (int x : new int[]{0, 14}) for (int z : new int[]{0, 14})
            open.add(cube(x, bottom, z, x + 2, top, z + 2, "#casing"));
        model.add("elements", open);
        model.remove("groups");
    }

    static JsonObject shaftModel(String texture, boolean item) {
        return shaftModel(texture, "create:block/gearbox", item);
    }

    static JsonObject shaftItem(String casing, String opening, ShaftBuilder shaft) {
        JsonObject model = shaftModel(casing, opening, true);
        model.getAsJsonObject("textures").addProperty("1_0", shaft.shaftTexture());
        model.getAsJsonObject("textures").addProperty("1_1", shaft.shaftTopTexture());
        return model;
    }

    static JsonObject shaftModel(String texture, String shaft, boolean item) {
        JsonObject model = GearAssets.parent("create:block/encased_shaft/" + (item ? "item" : "block"));
        JsonObject slots = new JsonObject();
        slots.addProperty("casing", texture);
        slots.addProperty("particle", texture);
        slots.addProperty("opening", shaft);
        model.add("textures", slots);
        return model;
    }

    static JsonObject pipeModel(String texture, boolean open) {
        return pipeModel(texture, null, open);
    }

    static JsonObject pipeModel(String texture, String pipe, boolean open) {
        return pipeModel(texture, pipe, "create:block/pipes", open);
    }

    static JsonObject pipeModel(String texture, String frame, String material, boolean open) {
        JsonObject model = GearAssets.parent("create:block/encased_fluid_pipe/" + (open ? "block_open" : "block_flat"));
        JsonObject slots = new JsonObject();
        slots.addProperty("0", open ? frame == null ? "create:block/encased_pipe" : frame : texture);
        slots.addProperty("particle", texture);
        model.add("textures", slots);
        if (open) {
            slots.addProperty("pipe", material);
            JsonArray elements = new JsonArray();
            pipeOpening(elements, "south");
            model.add("elements", elements);
        }
        return model;
    }

    private static JsonObject pipeItem(String texture) {
        return pipeItem(texture, null);
    }

    private static JsonObject pipeItem(String texture, String pipe) {
        return pipeItem(texture, pipe, "create:block/pipes");
    }

    private static JsonObject pipeItem(String texture, String frame, String material) {
        JsonObject model = GearAssets.parent("minecraft:block/cube");
        JsonObject slots = new JsonObject();
        slots.addProperty("particle", texture);
        slots.addProperty("0", frame == null ? "create:block/encased_pipe" : frame);
        slots.addProperty("pipe", material);
        JsonArray elements = new JsonArray();
        for (String direction : new String[]{"down", "up", "west", "east"}) {
            slots.addProperty(direction, texture);
            JsonObject element = cube(0, 0, 0, 16, 16, 16, "#" + direction);
            JsonObject face = element.getAsJsonObject("faces").getAsJsonObject(direction);
            JsonObject faces = new JsonObject();
            faces.add(direction, face);
            element.add("faces", faces);
            elements.add(element);
        }
        pipeOpening(elements, "north");
        pipeOpening(elements, "south");
        model.add("textures", slots);
        model.add("elements", elements);
        return model;
    }

    /** Native pipe ends occupy pixels 3..12 of a casing face. Keep one shared frame around them. */
    private static void pipeOpening(JsonArray elements, String direction) {
        pipePanel(elements, direction, 0, 0, 16, 3, false);
        pipePanel(elements, direction, 0, 13, 16, 16, false);
        pipePanel(elements, direction, 0, 3, 3, 13, false);
        pipePanel(elements, direction, 13, 3, 16, 13, false);
        pipePanel(elements, direction, 3, 3, 13, 13, true);
    }

    private static void pipePanel(JsonArray elements, String direction, int u0, int v0, int u1, int v1,
                                  boolean pipe) {
        boolean south = direction.equals("south");
        JsonObject element = new JsonObject();
        element.add("from", array(south ? u0 : 16 - u1, 16 - v1, south ? 15 : 0));
        element.add("to", array(south ? u1 : 16 - u0, 16 - v0, south ? 16 : 1));
        JsonObject face = new JsonObject();
        face.addProperty("texture", pipe ? "#pipe" : "#0");
        // Create's 32x32 pipe atlas stores the ten-pixel end at normalized UVs 11..16.
        face.add("uv", pipe ? array(11, 11, 16, 16) : array(u0, v0, u1, v1));
        JsonObject faces = new JsonObject();
        faces.add(direction, face);
        element.add("faces", faces);
        elements.add(element);
    }

    private static JsonObject cube(int x0, int y0, int z0, int x1, int y1, int z1, String texture) {
        JsonObject cube = new JsonObject();
        cube.add("from", array(x0, y0, z0));
        cube.add("to", array(x1, y1, z1));
        JsonObject faces = new JsonObject();
        for (String direction : new String[]{"north", "south", "west", "east", "up", "down"}) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", texture);
            faces.add(direction, face);
        }
        cube.add("faces", faces);
        return cube;
    }

    private static JsonArray array(int... values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(value);
        return array;
    }

    static JsonObject axisState(ResourceLocation id, boolean shaft) {
        JsonObject variants = new JsonObject();
        String root = id.getNamespace() + ":block/" + id.getPath() + "/block";
        for (String axis : new String[]{"x", "y", "z"}) {
            if (shaft) variants.add("axis=" + axis, axisVariant(root, axis, true));
            else for (boolean bottom : new boolean[]{false, true}) for (boolean top : new boolean[]{false, true})
                variants.add("axis=" + axis + ",bottom_shaft=" + bottom + ",top_shaft=" + top,
                        axisVariant(root + GearAssets.suffix(bottom, top), axis, false));
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }

    private static JsonObject axisVariant(String model, String axis, boolean uvlock) {
        JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        if (!axis.equals("y")) {
            variant.addProperty("x", 90);
            variant.addProperty("y", axis.equals("x") ? 90 : 180);
        }
        if (uvlock) variant.addProperty("uvlock", true);
        return variant;
    }

    static JsonObject pipeState(ResourceLocation id) {
        String[] directions = {"down", "up", "north", "south", "west", "east"};
        int[] x = {-90, 90, 0, 0, 0, 0};
        int[] y = {0, 0, 180, 0, 90, 270};
        JsonArray parts = new JsonArray();
        for (boolean open : new boolean[]{false, true}) for (int i = 0; i < directions.length; i++) {
            JsonObject part = new JsonObject();
            JsonObject when = new JsonObject();
            when.addProperty(directions[i], Boolean.toString(open));
            JsonObject apply = new JsonObject();
            apply.addProperty("model", id.getNamespace() + ":block/" + id.getPath() + (open ? "/block_open" : "/block_flat"));
            if (x[i] != 0) apply.addProperty("x", x[i]);
            if (y[i] != 0) apply.addProperty("y", y[i]);
            part.add("when", when);
            part.add("apply", apply);
            parts.add(part);
        }
        JsonObject state = new JsonObject();
        state.add("multipart", parts);
        return state;
    }
}
