package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Copies installed native model declarations, preserving OBJ loaders and their material slots. */
final class BlazeBurnerAssets {
    static final Map<String, String> NATIVE_PARTS = new LinkedHashMap<>();
    static {
        NATIVE_PARTS.put("blaze", "blaze_burner/blaze/idle");
        NATIVE_PARTS.put("blaze_active", "blaze_burner/blaze/active");
        NATIVE_PARTS.put("blaze_super", "blaze_burner/blaze/super");
        NATIVE_PARTS.put("blaze_super_active", "blaze_burner/blaze/super_active");
        NATIVE_PARTS.put("inert", "blaze_burner/blaze/inert");
        NATIVE_PARTS.put("rods", "blaze_burner/rods_small");
        NATIVE_PARTS.put("rods_2", "blaze_burner/rods_large");
        NATIVE_PARTS.put("super_rods", "blaze_burner/superheated_rods_small");
        NATIVE_PARTS.put("super_rods_2", "blaze_burner/superheated_rods_large");
        NATIVE_PARTS.put("flame", "blaze_burner/flame");
        NATIVE_PARTS.put("goggles", "blaze_burner/goggles");
        NATIVE_PARTS.put("goggles_small", "blaze_burner/goggles_small");
        NATIVE_PARTS.put("train_hat", "train_hat");
        NATIVE_PARTS.put("logistics_hat", "logistics_hat");
    }
    static void generate(BlazeBurnerBuilder builder, KubeAssetGenerator generator) {
        String root = "block/" + builder.id.getPath() + "/";
        GearAssets.put(generator, builder.id, "models", root + "block", model(builder, "block", "blaze_burner/block"));
        for (var part : NATIVE_PARTS.entrySet())
            GearAssets.put(generator, builder.id, "models", root + part.getKey(), model(builder, part.getKey(), part.getValue()));
        GearAssets.put(generator, builder.id, "models", root + "fire", model(builder, "fire", "blaze_burner/block_with_fire"));
        GearAssets.put(generator, builder.id, "models", root + "soul_fire", model(builder, "soul_fire", "blaze_burner/block_with_soul_fire"));
        JsonObject variants = new JsonObject(); variants.add("", variant(builder, "block"));
        JsonObject state = new JsonObject(); state.add("variants", variants);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), builder.blockstateJson == null ? state : builder.blockstateJson);
        variants = new JsonObject(); variants.add("flame_type=regular", variant(builder, "fire")); variants.add("flame_type=soul", variant(builder, "soul_fire"));
        state = new JsonObject(); state.add("variants", variants);
        GearAssets.put(generator, builder.litId(), "blockstates", builder.litId().getPath(), state);
        if (builder.itemBuilder != null) {
            JsonObject item = builder.modelOverrides.usesPrimaryItem() ? GearAssets.parent(builder.model)
                    : itemModel(builder);
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), GearAssets.itemOverrides(item,
                    CBJSAssets.itemModel(builder.itemBuilder), builder.modelOverrides.usesPrimaryItem() ? null : CBJSAssets.parent(builder.itemBuilder), CBJSAssets.itemTextures(builder.itemBuilder)));
        }
        GearAssets.put(generator, builder.emptyId(), "models/item", builder.emptyId().getPath(), model(builder, "empty_item", "blaze_burner/block"));
    }
    private static JsonObject variant(BlazeBurnerBuilder builder, String role) {
        JsonObject variant = new JsonObject(); variant.addProperty("model", builder.id.getNamespace() + ":block/" + builder.id.getPath() + "/" + role); return variant;
    }
    private static JsonObject model(BlazeBurnerBuilder builder, String role, String nativePath) {
        if (!builder.options().renders(role)) {
            if (role.equals("fire") || role.equals("soul_fire")) return model(builder, "block", "blaze_burner/block");
            JsonObject hidden = new JsonObject(); hidden.add("elements", new JsonArray()); return hidden;
        }
        JsonObject model;
        if (role.equals("block") && builder.modelOverrides.customPrimary()) model = builder.modelOverrides.primaryModel("create:block/" + nativePath);
        else if (builder.modelOverrides.has(role)) model = builder.modelOverrides.partModel(role, "create:block/" + nativePath);
        else model = nativeModel(nativePath);
        JsonObject textures = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        for (var entry : textures.entrySet()) {
            String texture = entry.getValue().getAsString();
            String textureRole = switch (texture) {
                case "create:block/blaze_heater_brazier" -> "brazier";
                case "create:block/blaze_heater_brazier_soul" -> "brazier_soul";
                case "create:block/blaze_idle" -> "blaze";
                case "create:block/blaze_active" -> "blaze_active";
                case "create:block/blaze_super" -> "blaze_super";
                case "create:block/blaze_super_active" -> "blaze_super_active";
                case "create:block/blaze_inert" -> "inert";
                case "create:block/blaze_burner_flame" -> "flame";
                case "create:block/hat_test" -> "goggles";
                case "block/campfire_fire", "minecraft:block/campfire_fire" -> "fire";
                case "block/soul_campfire_fire", "minecraft:block/soul_campfire_fire" -> "soul_fire";
                case "create:block/dark_metal_block" -> "particle";
                default -> role;
            };
            if (NATIVE_PARTS.containsKey(role) && builder.textures.containsKey(role)) entry.setValue(new com.google.gson.JsonPrimitive(builder.textures.get(role)));
            else if (builder.textures.containsKey(textureRole)) entry.setValue(new com.google.gson.JsonPrimitive(builder.textures.get(textureRole)));
            else if (builder.textures.containsKey("all")) entry.setValue(new com.google.gson.JsonPrimitive(builder.textures.get("all")));
            else if (builder.allTexture != null) entry.setValue(new JsonPrimitive(builder.allTexture));
        }
        for (var entry : builder.textures.entrySet()) textures.addProperty(entry.getKey(), entry.getValue());
        if (!textures.entrySet().isEmpty()) model.add("textures", textures);
        return model;
    }
    private static JsonObject itemModel(BlazeBurnerBuilder builder) {
        if (builder.options().haveBlaze && builder.options().haveRods)
            return model(builder, "filled_item", "blaze_burner/block_with_blaze");
        if (!builder.options().haveBlaze && !builder.options().haveRods)
            return model(builder, "block", "blaze_burner/block");
        JsonObject result = new JsonObject();
        result.addProperty("parent", "minecraft:block/block");
        result.addProperty("loader", "neoforge:composite");
        JsonObject children = new JsonObject();
        children.add("body", model(builder, "block", "blaze_burner/block"));
        if (builder.options().haveBlaze) children.add("blaze", model(builder, "blaze", "blaze_burner/blaze/idle"));
        if (builder.options().haveRods) {
            children.add("rods", model(builder, "rods", "blaze_burner/rods_small"));
            JsonObject largeRods = model(builder, "rods_2", "blaze_burner/rods_large");
            // Native item.obj places these rods lower than their animated world partial.
            if (!builder.modelOverrides.has("rods_2") && !largeRods.has("transform")) {
                JsonArray translation = new JsonArray(); translation.add(0); translation.add(-0.2); translation.add(0);
                JsonObject transform = new JsonObject(); transform.add("translation", translation);
                largeRods.add("transform", transform);
            }
            children.add("rods_2", largeRods);
        }
        result.add("children", children);
        result.add("display", nativeModel("blaze_burner/block_with_blaze").get("display").deepCopy());
        return result;
    }
    private static JsonObject nativeModel(String path) {
        return ScriptModelResources.load(ResourceLocation.parse("create:" + (path.endsWith("_hat") ? "entity/" : "block/") + path)).deepCopy();
    }
    static JsonObject loot(BlazeBurnerBuilder builder) {
        JsonObject loot = GearAssets.defaultLoot(builder.emptyId());
        JsonObject pool = loot.getAsJsonArray("pools").get(0).getAsJsonObject();
        JsonObject condition = new JsonObject(); condition.addProperty("condition", "minecraft:block_state_property"); condition.addProperty("block", builder.id.toString());
        JsonObject properties = new JsonObject(); properties.addProperty("blaze", "none"); condition.add("properties", properties);
        JsonObject empty = pool.getAsJsonArray("entries").get(0).getAsJsonObject();
        JsonArray conditions = new JsonArray(); conditions.add(condition); empty.add("conditions", conditions);
        if (builder.itemBuilder != null) {
            JsonObject filled = new JsonObject(); filled.addProperty("type", "minecraft:item"); filled.addProperty("name", builder.id.toString());
            JsonObject inverted = new JsonObject(); inverted.addProperty("condition", "minecraft:inverted"); inverted.add("term", condition.deepCopy());
            conditions = new JsonArray(); conditions.add(inverted); filled.add("conditions", conditions); pool.getAsJsonArray("entries").add(filled);
        }
        return loot;
    }
    private BlazeBurnerAssets() { }
}
