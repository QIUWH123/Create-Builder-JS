package net.quiwh.cbjs.kubejs;

import com.google.gson.*;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.GearModelSet;
import java.util.Map;

/** Generates every view from the same shaftless wheel and optional shaft parts. */
final class GearAssets {
    private GearAssets() { }
    static ResourceLocation path(ResourceLocation id, String path) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }
    static JsonObject load(ResourceLocation model) { return ScriptModelResources.load(model); }
    static JsonObject resolved(ResourceLocation id) { return ScriptModelResources.resolved(id); }
    static ResourceLocation partSource(String part) {
        return switch (part) {
            case "shaft" -> GearModelSet.COGWHEEL_SHAFT;
            case "shaft_half" -> GearModelSet.SHAFT_HALF;
            default -> ResourceLocation.fromNamespaceAndPath("create", "block/" + part);
        };
    }
    static boolean customShaftTextures(CogwheelBuilder builder) {
        return builder.allTexture != null || builder.textures.keySet().stream().anyMatch(key -> !key.equals("gear"));
    }
    static JsonObject model(CogwheelBuilder builder, String suffix, boolean casing) {
        String role = suffix.contains("shaftless") ? "cogwheel" : suffix;
        if (builder.modelOverrides().has(role)) {
            return customModel(builder, builder.modelOverrides().partModel(role, ""));
        }
        ResourceLocation model = builder.flanged && suffix.contains("shaftless")
                ? ResourceLocation.fromNamespaceAndPath("cbjs", "block/flanged_cogwheel/"
                        + (suffix.startsWith("large_") ? "large" : "small") + "_shaftless")
                : partSource(suffix);
        JsonObject json = resolved(model);
        applyTextures(json, CBJSAssets.json(builder.textures), casing,
                !suffix.equals("shaft") && !suffix.equals("shaft_half"), builder.allTexture);
        JsonObject alias = parent(model.toString());
        alias.add("textures", json.getAsJsonObject("textures"));
        return alias;
    }
    static void applyTextures(JsonObject json, JsonObject overrides, boolean casing, boolean gearPart) {
        applyTextures(json, overrides, casing, gearPart, null);
    }
    static void applyTextures(JsonObject json, JsonObject overrides, boolean casing, boolean gearPart, String allTexture) {
        JsonObject textures = json.has("textures") ? json.getAsJsonObject("textures") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            String value = entry.getValue().getAsString();
            String key = null;
            if (value.contains("axis_top")) key = "shaft_top";
            else if (value.contains("axis") || value.contains("shaft")) key = "shaft";
            else if (!casing && gearPart) key = "gear";
            if (allTexture != null) entry.setValue(new JsonPrimitive(allTexture));
            if (key != null && overrides.has(key)) entry.setValue(overrides.get(key));
        }
        // Retain ordinary .replaceTexture(actualSlot, ...) in addition to semantic gear aliases.
        for (var entry : overrides.entrySet())
            if (!entry.getKey().equals("gear") && !entry.getKey().equals("shaft") && !entry.getKey().equals("shaft_top"))
                textures.add(entry.getKey(), entry.getValue());
        json.add("textures", textures);
    }
    static JsonObject encasedItem(boolean large, boolean brass, JsonObject overrides, String allTexture) {
        return CasingAssets.materialItem(large, brass, overrides, allTexture);
    }
    static JsonObject itemOverrides(JsonObject item, JsonObject explicitModel, String parent, JsonObject textures) {
        if (explicitModel != null) item = explicitModel.deepCopy();
        else if (parent != null && !parent.isEmpty()) item = parent(parent);
        mergeTextures(item, textures);
        return item;
    }
    static void mergeTextures(JsonObject model, JsonObject overrides) {
        if (overrides.entrySet().isEmpty()) return;
        JsonObject target = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        for (var entry : overrides.entrySet()) target.add(entry.getKey(), entry.getValue().deepCopy());
        model.add("textures", target);
    }
    static void put(KubeAssetGenerator generator, ResourceLocation id, String kind, String name, JsonObject json) {
        generator.json(path(id, kind + "/" + name), json);
    }
    static void generate(CogwheelBuilder builder, KubeAssetGenerator generator) {
        builder.validateModels();
        String root = "block/" + builder.id.getPath();
        if (builder.base == null) {
            for (String part : new String[]{"cogwheel_shaftless", "large_cogwheel_shaftless"})
                put(generator, builder.id, "models", root + "/" + part, model(builder, part, false));
            if (!builder.shaftless)
                for (String part : new String[]{"shaft", "shaft_half"}) if (customShaftTextures(builder) || builder.modelOverrides().has(part))
                    put(generator, builder.id, "models", root + "/" + part, model(builder, part, false));
            put(generator, builder.id, "models", root, composedGear(builder));
        } else {
            for (boolean bottom : new boolean[]{false, true}) for (boolean top : new boolean[]{false, true}) {
                String suffix = suffix(bottom, top);
                put(generator, builder.id, "models", root + "/block" + suffix,
                        CasingAssets.stockCogwheelModel(builder.large, builder.shaftless ? "" : suffix, builder.brass));
            }
        }
        JsonObject variants = new JsonObject();
        for (String axis : new String[]{"x", "y", "z"}) {
            if (builder.base == null) variants.add("axis=" + axis, variant(builder, root, axis));
            else for (boolean bottom : new boolean[]{false, true}) for (boolean top : new boolean[]{false, true})
                variants.add("axis=" + axis + ",bottom_shaft=" + bottom + ",top_shaft=" + top,
                        variant(builder, root + "/block" + suffix(bottom, top), axis));
        }
        JsonObject state = new JsonObject(); state.add("variants", variants);
        put(generator, builder.id, "blockstates", builder.id.getPath(), builder.blockstateJson == null ? state : builder.blockstateJson);
        if (builder.itemBuilder != null) {
            JsonObject item = builder.base == null ? parent(builder.id.getNamespace() + ":" + root)
                    : customWheelModel(builder.base)
                        ? encasedComposite(builder.base, builder.large, builder.brass)
                        : encasedItem(builder.large, builder.brass, CBJSAssets.json(builder.base.textures), builder.base.allTexture);
            String itemParent = CBJSAssets.parent(builder.itemBuilder);
            item = itemOverrides(item, CBJSAssets.itemModel(builder.itemBuilder), itemParent,
                    CBJSAssets.itemTextures(builder.itemBuilder));
            put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }
    private static JsonObject customModel(CogwheelBuilder builder, JsonObject model) {
        // External parents resolve during resource loading. Only script-supplied slots
        // are added; unknown authored defaults are never read or replaced at startup.
        mergeTextures(model, CBJSAssets.json(builder.textures));
        return model;
    }
    static boolean customWheelModel(CogwheelBuilder builder) {
        return builder.flanged || builder.modelOverrides().has("cogwheel");
    }

    private static JsonObject composedGear(CogwheelBuilder builder) {
        var models = builder.models();
        return composite(builder.large ? "create:block/large_wheels" : "minecraft:block/block",
                parent((builder.large ? models.large() : models.small()).toString()),
                builder.shaftless ? null : parent(models.shaft().toString()));
    }

    static JsonObject encasedComposite(CogwheelBuilder builder, boolean large, boolean brass) {
        JsonObject shell = CasingAssets.stockCogwheelModel(large, "", brass);
        return encasedComposite(builder, large, shell, "create:block/" + (brass ? "brass" : "andesite") + "_casing");
    }

    static JsonObject encasedComposite(CogwheelBuilder builder, boolean large, JsonObject shell, String particle) {
        JsonObject item = composite(large ? "create:block/large_wheels" : "minecraft:block/block",
                parent((large ? builder.models().large() : builder.models().small()).toString()), null);
        // Keep child texture dictionaries independent: casing slots must not replace wheel slots.
        item.getAsJsonObject("children").add("casing", shell);
        item.getAsJsonObject("textures").addProperty("particle", particle);
        return item;
    }

    private static JsonObject composite(String displayParent, JsonObject wheel, JsonObject shaft) {
        JsonObject json = parent(displayParent);
        json.addProperty("loader", "neoforge:composite");
        JsonObject textures = new JsonObject(); textures.addProperty("particle", "minecraft:block/stripped_spruce_log_top");
        json.add("textures", textures);
        JsonObject children = new JsonObject(); children.add("cogwheel", wheel);
        if (shaft != null) children.add("shaft", shaft);
        json.add("children", children);
        return json;
    }
    static JsonObject defaultLoot(ResourceLocation drop) {
        JsonObject loot = new JsonObject(); loot.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray(); loot.add("pools", pools);
        if (drop != null) {
            JsonObject pool = new JsonObject(); pool.addProperty("rolls", 1);
            JsonArray entries = new JsonArray(); JsonObject entry = new JsonObject();
            entry.addProperty("type", "minecraft:item"); entry.addProperty("name", drop.toString());
            entries.add(entry); pool.add("entries", entries);
            JsonArray conditions = new JsonArray(); JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:survives_explosion"); conditions.add(condition);
            pool.add("conditions", conditions); pools.add(pool);
        }
        return loot;
    }
    static String suffix(boolean bottom, boolean top) { return top ? bottom ? "_top_bottom" : "_top" : bottom ? "_bottom" : ""; }
    static JsonObject parent(String parent) { JsonObject json = new JsonObject(); json.addProperty("parent", parent); return json; }
    static JsonObject variant(CogwheelBuilder builder, String model, String axis) {
        JsonObject json = new JsonObject(); json.addProperty("model", builder.id.getNamespace() + ":" + model);
        if (!axis.equals("y")) { json.addProperty("x", 90); json.addProperty("y", axis.equals("x") ? 90 : 180); }
        return json;
    }
}
