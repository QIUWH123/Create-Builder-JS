package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;

/** Horizontal vaults use Create's model; upright silos assign their own six face roles. */
final class ItemVaultAssets {
    static void generate(ItemVaultBuilder builder, AssetJsonGenerator generator) {
        StorageAssetOverrides.validate(builder);
        DeviceTextureAtlas.generate(builder, generator);
        String root = "block/" + builder.id.getPath();
        JsonObject model = builder.vertical() ? siloModel() : GearAssets.parent("create:block/item_vault");
        JsonObject textures = new JsonObject();
        textures.addProperty("0", builder.spritePath("vault_bottom_small"));
        textures.addProperty("1", builder.spritePath("vault_front_small"));
        if (!builder.vertical()) textures.addProperty("2", builder.spritePath("vault_side_small"));
        textures.addProperty("3", builder.spritePath("vault_top_small"));
        textures.addProperty("particle", builder.spritePath("vault_top_small"));
        model.add("textures", textures);
        GearAssets.put(generator, builder.id, "models", root, model);
        JsonObject variants = new JsonObject();
        for (boolean large : new boolean[]{false, true}) {
            if (builder.vertical()) variants.add("large=" + large, variant(builder, root, 0, 0));
            else for (String axis : new String[]{"x", "z"})
                variants.add("axis=" + axis + ",large=" + large, variant(builder, root, 0, axis.equals("x") ? 90 : 0));
        }
        JsonObject state = new JsonObject(); state.add("variants", variants);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), state);
        if (builder.itemBuilder != null) {
            JsonObject item = GearAssets.parent(builder.id.getNamespace() + ":" + root);
            item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson, builder.itemBuilder.parentModel, builder.itemBuilder.textureJson);
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }
    private static JsonObject siloModel() {
        JsonObject model = GearAssets.parent("minecraft:block/block");
        JsonObject cube = new JsonObject();
        cube.add("from", numbers(0, 0, 0));
        cube.add("to", numbers(16, 16, 16));
        JsonObject faces = new JsonObject();
        for (String side : new String[]{"down", "up", "north", "south", "west", "east"}) {
            JsonObject face = new JsonObject();
            face.add("uv", numbers(0, 0, 16, 16));
            face.addProperty("texture", side.equals("down") ? "#0" : side.equals("up") ? "#1" : "#3");
            faces.add(side, face);
        }
        cube.add("faces", faces);
        JsonArray elements = new JsonArray(); elements.add(cube); model.add("elements", elements);
        JsonObject gui = new JsonObject();
        gui.add("rotation", numbers(30, 135, 0));
        gui.add("scale", numbers(0.625, 0.625, 0.625));
        JsonObject display = new JsonObject(); display.add("gui", gui); model.add("display", display);
        return model;
    }
    private static JsonArray numbers(double... values) {
        JsonArray result = new JsonArray();
        for (double value : values) result.add(value);
        return result;
    }
    private static JsonObject variant(ItemVaultBuilder builder, String root, int x, int y) {
        JsonObject result = new JsonObject(); result.addProperty("model", builder.id.getNamespace() + ":" + root);
        if (x != 0) result.addProperty("x", x);
        if (y != 0) result.addProperty("y", y);
        return result;
    }
    private ItemVaultAssets() { }
}
