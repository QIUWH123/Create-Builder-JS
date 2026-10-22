package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;

/** Generated tank models compose bounded per-face fragments at render time. */
final class FluidTankAssets {
    static void generate(FluidTankBuilder builder, AssetJsonGenerator generator) {
        StorageAssetOverrides.validate(builder);
        DeviceTextureAtlas.generate(builder, generator);
        FluidTankFaceAssets.generate(builder, generator);
        String root = "block/" + builder.id.getPath() + "/";
        JsonObject state = new JsonObject();
        JsonObject variants = new JsonObject();
        JsonObject variant = new JsonObject();
        variant.addProperty("model", builder.id.getNamespace() + ":" + root + "single");
        variants.add("", variant); state.add("variants", variants);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), state);
        generateGauges(builder, generator, root);
        if (builder.itemBuilder == null) return;
        JsonObject item = GearAssets.parent(builder.id.getNamespace() + ":" + root + "single");
        item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson,
                builder.itemBuilder.parentModel, builder.itemBuilder.textureJson);
        GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
    }

    static void generateGauges(FluidTankBuilder builder, AssetJsonGenerator generator, String root) {
        for (String part : new String[]{"gauge", "gauge_dial"}) {
            JsonObject model = GearAssets.parent("create:block/steam_engine/" + part);
            JsonObject textures = new JsonObject();
            textures.addProperty("0", builder.spritePath("boiler_gauge"));
            textures.addProperty("particle", builder.spritePath("boiler_gauge"));
            model.add("textures", textures);
            GearAssets.put(generator, builder.id, "models", root + part, model);
        }
    }

    private FluidTankAssets() { }
}
