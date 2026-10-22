package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;

/** Pipe state aliases inherit Create's installed geometry and retain script-owned IDs. */
final class PipeAssets {
    private PipeAssets() { }

    static void generate(FluidPipeBuilder builder, AssetJsonGenerator generator) {
        ResourceLocation id = builder.id;
        String root = "block/" + id.getPath();
        FluidPipeBuilder source = builder.source();
        String particle = source.textures.has("particle") ? source.textures.get("particle").getAsString()
                : source.pipeTexture();
        JsonObject state;
        JsonObject item;
        switch (builder.kind) {
            case REGULAR -> {
                if (builder.modelJson != null || builder.model != null && !builder.model.isEmpty()) {
                    JsonObject body = builder.modelJson == null ? GearAssets.parent(builder.model) : builder.modelJson.deepCopy();
                    if (source.allTexture != null || source.textures.has("pipe_connected"))
                        textures(body).addProperty("0", source.connectedTexture());
                    if (source.allTexture != null || source.textures.has("pipe"))
                        textures(body).addProperty("1", source.pipeTexture());
                    if (source.textures.has("particle")) textures(body).add("particle", source.textures.get("particle"));
                    rawTextures(body, source.textures);
                    GearAssets.put(generator, id, "models", root, body);
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", id.getNamespace() + ":" + root);
                    JsonObject variants = new JsonObject();
                    variants.add("", variant);
                    state = new JsonObject();
                    state.add("variants", variants);
                    item = GearAssets.parent(id.getNamespace() + ":" + root);
                } else {
                    state = normalState(id);
                    for (JsonElement element : state.getAsJsonArray("multipart")) {
                        String model = element.getAsJsonObject().getAsJsonObject("apply").get("model").getAsString();
                        String part = model.substring(model.lastIndexOf('/') + 1);
                        JsonObject modelJson = normalModel(part, source.connectedTexture(), particle);
                        // Native rim/connector quads are injected after baking, so retain their atlases.
                        modelJson.getAsJsonObject("textures").addProperty("pipe", source.pipeTexture());
                        modelJson.getAsJsonObject("textures").addProperty("glass", source.glassTexture());
                        rawTextures(modelJson, source.textures);
                        GearAssets.put(generator, id, "models", root + "/" + part, modelJson);
                    }
                    item = itemModel(source.pipeTexture(), particle);
                    rawTextures(item, source.textures);
                }
            }
            case GLASS -> {
                state = glassState(id);
                GearAssets.put(generator, id, "models", root + "/block", glassModel(source.glassTexture(), particle));
                item = glassModel(source.glassTexture(), particle);
            }
            case ENCASED -> {
                state = CasingAssets.pipeState(id);
                GearAssets.put(generator, id, "models", root + "/block_flat",
                        CasingAssets.pipeModel("create:block/copper_casing", null, source.pipeTexture(), false));
                GearAssets.put(generator, id, "models", root + "/block_open",
                        CasingAssets.pipeModel("create:block/copper_casing", null, source.pipeTexture(), true));
                item = itemModel(source.pipeTexture(), particle);
            }
            default -> throw new IllegalArgumentException("Unsupported fluid pipe kind " + builder.kind);
        }
        if (builder.blockstateJson != null) state = builder.blockstateJson;
        GearAssets.put(generator, id, "blockstates", id.getPath(), state);
        if (builder.itemBuilder != null) {
            item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson, builder.itemBuilder.parentModel,
                    builder.itemBuilder.textureJson);
            GearAssets.put(generator, id, "models/item", id.getPath(), item);
        }
    }

    private static JsonObject textures(JsonObject model) {
        if (!model.has("textures")) model.add("textures", new JsonObject());
        return model.getAsJsonObject("textures");
    }

    private static void rawTextures(JsonObject model, JsonObject overrides) {
        for (var entry : overrides.entrySet()) {
            if (entry.getKey().equals("pipe") || entry.getKey().equals("pipe_connected") || entry.getKey().equals("glass")) continue;
            textures(model).add(entry.getKey(), entry.getValue());
        }
    }

    static JsonObject normalState(ResourceLocation id) {
        JsonObject state = ScriptModelResources.loadBlockstate(ResourceLocation.parse("create:fluid_pipe"));
        for (JsonElement element : state.getAsJsonArray("multipart")) {
            JsonObject apply = element.getAsJsonObject().getAsJsonObject("apply");
            String model = apply.get("model").getAsString();
            String part = model.substring(model.lastIndexOf('/') + 1);
            apply.addProperty("model", id.getNamespace() + ":block/" + id.getPath() + "/" + part);
        }
        return state;
    }

    static JsonObject normalModel(String part, String connectedTexture, String particle) {
        JsonObject model = GearAssets.parent("create:block/fluid_pipe/" + part);
        JsonObject textures = new JsonObject();
        textures.addProperty("0", connectedTexture);
        textures.addProperty("particle", particle);
        model.add("textures", textures);
        return model;
    }

    static JsonObject itemModel(String pipeTexture, String particle) {
        JsonObject model = GearAssets.parent("create:block/fluid_pipe/item");
        JsonObject textures = new JsonObject();
        textures.addProperty("1", pipeTexture);
        textures.addProperty("particle", particle);
        model.add("textures", textures);
        return model;
    }

    static JsonObject glassModel(String glassTexture, String particle) {
        JsonObject model = GearAssets.parent("create:block/fluid_pipe/window");
        JsonObject textures = new JsonObject();
        textures.addProperty("0", glassTexture);
        textures.addProperty("particle", particle);
        model.add("textures", textures);
        return model;
    }

    static JsonObject glassState(ResourceLocation id) {
        JsonObject variants = new JsonObject();
        for (boolean alt : new boolean[]{false, true}) for (String axis : new String[]{"x", "y", "z"}) {
            JsonObject model = new JsonObject();
            model.addProperty("model", id.getNamespace() + ":block/" + id.getPath() + "/block");
            if (!axis.equals("y")) model.addProperty("x", 90);
            if (axis.equals("x")) model.addProperty("y", 90);
            variants.add("alt=" + alt + ",axis=" + axis, model);
        }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }
}
