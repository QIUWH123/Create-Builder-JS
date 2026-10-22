package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;

/** Editable KubeJS six-way casing with native Create shaft partials. */
final class GearboxVariantAssets {
    private static final String DEFAULT_MODEL = "kubejs:block/cogwheel/six_way_gearbox/block";

    static void generate(GearboxBuilder builder, AssetJsonGenerator generator) {
        JsonObject body = textureModel(builder, builder.modelOverrides.primaryModel(DEFAULT_MODEL));
        GearAssets.put(generator, builder.id, "models", "block/" + builder.id.getPath() + "/block", body);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), builder.blockstateJson == null
                ? CasingAssets.axisState(builder.id, true) : builder.blockstateJson);
        if (builder.itemBuilder != null)
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), builder.itemModel(itemModel(builder)));
    }

    /** The authored six-way model has one surface atlas, slot zero. */
    static JsonObject textureModel(GearboxBuilder builder, JsonObject model) {
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        String atlas = builder.textureSpecified("gearbox") ? builder.gearboxTexture()
                : builder.textureSpecified("casing") ? builder.casingTexture() : null;
        if (atlas != null) {
            slots.addProperty("0", atlas);
            slots.addProperty("particle", atlas);
        }
        for (var entry : builder.textures.entrySet())
            if (!java.util.Set.of("casing", "gearbox", "all", "north").contains(entry.getKey()))
                slots.add(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
        return model;
    }

    private static JsonObject itemModel(GearboxBuilder builder) {
        JsonObject model = textureModel(builder, GearAssets.parent(DEFAULT_MODEL));
        model.addProperty("loader", "forge:composite");
        JsonObject children = new JsonObject();
        children.add("body", textureModel(builder, GearAssets.parent(DEFAULT_MODEL)));
        children.add("shafts", itemShafts());
        model.add("children", children);
        return model;
    }

    /** Item models cannot animate partials; three axis rods show the six shaft ends. */
    private static JsonObject itemShafts() {
        JsonObject model = GearAssets.parent("minecraft:block/block");
        JsonObject textures = new JsonObject();
        textures.addProperty("0", "create:block/axis");
        textures.addProperty("1", "create:block/axis_top");
        model.add("textures", textures);
        JsonArray elements = new JsonArray();
        String[] names = {"west", "east", "down", "up", "north", "south"};
        for (int axis = 0; axis < 3; axis++) {
            int[] from = {6, 6, 6};
            int[] to = {10, 10, 10};
            from[axis] = 0;
            to[axis] = 16;
            JsonObject element = new JsonObject();
            element.add("from", vector(from));
            element.add("to", vector(to));
            JsonObject faces = new JsonObject();
            for (int faceIndex = 0; faceIndex < names.length; faceIndex++) {
                JsonObject face = new JsonObject();
                face.addProperty("texture", faceIndex / 2 == axis ? "#1" : "#0");
                faces.add(names[faceIndex], face);
            }
            element.add("faces", faces);
            elements.add(element);
        }
        model.add("elements", elements);
        return model;
    }

    private static JsonArray vector(int[] values) {
        JsonArray vector = new JsonArray();
        for (int value : values) vector.add(value);
        return vector;
    }

    private GearboxVariantAssets() { }
}
