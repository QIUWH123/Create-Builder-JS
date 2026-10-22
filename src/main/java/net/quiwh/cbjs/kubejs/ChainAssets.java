package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;

/** Reuses vanilla chain geometry and its exact axis rotations without copying assets. */
final class ChainAssets {
    static void generate(ChainBuilder builder, AssetJsonGenerator generator) {
        String modelPath = "block/" + builder.id.getPath();
        JsonObject model = builder.modelOverrides.primaryModel("minecraft:block/chain");
        applyTextures(model, builder, builder.modelOverrides.customPrimary());
        GearAssets.put(generator, builder.id, "models", modelPath, model);

        JsonObject variants = new JsonObject();
        for (String axis : new String[]{"x", "y", "z"}) {
            JsonObject variant = new JsonObject();
            variant.addProperty("model", builder.id.getNamespace() + ":" + modelPath);
            if (!axis.equals("y")) variant.addProperty("x", 90);
            if (axis.equals("x")) variant.addProperty("y", 90);
            variants.add("axis=" + axis, variant);
        }
        JsonObject blockstate = new JsonObject();
        blockstate.add("variants", variants);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(),
                builder.blockstateJson == null ? blockstate : builder.blockstateJson);
        if (builder.itemBuilder != null) {
            JsonObject item = GearAssets.parent("minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", builder.itemTexture());
            item.add("textures", textures);
            String itemParent = builder.itemBuilder.parentModel;
            if (builder.modelOverrides.usesPrimaryItem()) {
                item = GearAssets.parent(itemParent);
                applyTextures(item, builder, true);
                itemParent = "";
            }
            item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson, itemParent, builder.itemBuilder.textureJson);
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }

    private static void applyTextures(JsonObject model, ChainBuilder builder, boolean customModel) {
        JsonObject textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        if (!customModel || builder.textures.has("chain") || builder.textures.has("all")) {
            textures.addProperty("all", builder.chainTexture());
            textures.addProperty("particle", builder.chainTexture());
        }
        for (var entry : builder.textures.entrySet())
            if (!entry.getKey().equals("chain") && !entry.getKey().equals("all") && !entry.getKey().equals("item"))
                textures.add(entry.getKey(), entry.getValue());
        if (textures.size() != 0) model.add("textures", textures);
    }

    private ChainAssets() { }
}
