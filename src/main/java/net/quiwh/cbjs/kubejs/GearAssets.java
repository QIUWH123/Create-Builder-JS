package net.quiwh.cbjs.kubejs;

import com.google.gson.*;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.GearModelSet;
import java.util.Map;

/** Generates aliases for every stationary and moving model; imported geometry stays intact. */
final class GearAssets {
    private GearAssets() { }
    /** Recognize complete editable model-set paths; arbitrary KubeJS model paths stay untouched. */
    static String presetFromModel(String model) {
        return presetFromModel(model, false);
    }
    static String presetFromModel(String model, boolean shaftless) {
        String prefix = "kubejs:block/cogwheel/";
        if (model == null || !model.startsWith(prefix)) return null;
        String[] parts = model.substring(prefix.length()).split("/", -1);
        if (parts.length == 2 && parts[0].matches("[a-z0-9_]+")
                && (parts[1].equals("cogwheel") || parts[1].equals("large_cogwheel")
                    || shaftless && (parts[1].equals("cogwheel_shaftless") || parts[1].equals("large_cogwheel_shaftless")))
                && ScriptModelResources.exists(partSource(parts[0], "cogwheel_shaftless"))
                && ScriptModelResources.exists(ResourceLocation.parse(model)))
            return parts[0];
        return null;
    }
    static void validatePreset(String name) {
        if (name == null || !name.matches("[a-z0-9_]+") ||
                !ScriptModelResources.exists(partSource(name, "cogwheel_shaftless")))
            throw new IllegalArgumentException("Unknown imported cogwheel model set: " + name);
    }
    static ResourceLocation path(ResourceLocation id, String path) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }
    static JsonObject load(ResourceLocation model) { return ScriptModelResources.load(model); }
    static JsonObject resolved(ResourceLocation id) { return ScriptModelResources.resolved(id); }
    static ResourceLocation partSource(String preset, String part) {
        return switch (part) {
            case "shaft" -> GearModelSet.COGWHEEL_SHAFT;
            case "shaft_half" -> GearModelSet.SHAFT_HALF;
            default -> preset == null ? ResourceLocation.fromNamespaceAndPath("create", "block/" + part)
                    : ResourceLocation.fromNamespaceAndPath("kubejs", "block/cogwheel/" + preset + "/" + part);
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
        ResourceLocation model = partSource(builder.importedModel, suffix);
        JsonObject json = resolved(model);
        applyTextures(json, builder.textures, casing,
                !suffix.equals("shaft") && !suffix.equals("shaft_half"), builder.allTexture);
        if (builder.importedModel == null || suffix.equals("shaft") || suffix.equals("shaft_half")) {
            JsonObject alias = parent(model.toString());
            alias.add("textures", json.getAsJsonObject("textures"));
            return alias;
        }
        return json;
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
    static JsonObject encasedItem(String preset, boolean large, boolean brass, JsonObject overrides, String allTexture) {
        return CasingAssets.materialItem(preset, large, brass, overrides, allTexture);
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
    static void put(AssetJsonGenerator generator, ResourceLocation id, String kind, String name, JsonObject json) {
        generator.json(path(id, kind + "/" + name), json);
    }
    static void generate(CogwheelBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath();
        String size = builder.large ? "large_cogwheel" : "cogwheel";
        if (builder.base == null) {
            for (String part : new String[]{"cogwheel_shaftless", "large_cogwheel_shaftless"})
                put(generator, builder.id, "models", root + "/" + part, model(builder, part, false));
            if (!builder.shaftless)
                for (String part : new String[]{"shaft", "shaft_half"}) if (customShaftTextures(builder) || builder.modelOverrides().has(part))
                    put(generator, builder.id, "models", root + "/" + part, model(builder, part, false));
            JsonObject stationary;
            if (builder.modelJson != null) stationary = customModel(builder, builder.modelJson.deepCopy());
            else if (!builder.model.isEmpty()) stationary = customModel(builder, builder.modelOverrides().primaryModel(""));
            else if (customPartModels(builder)) stationary = composedGear(builder);
            else {
                // Presets keep their authored mesh; ordinary registrations inherit installed Create models.
                stationary = model(builder, size + (builder.shaftless ? "_shaftless" : ""), false);
            }
            put(generator, builder.id, "models", root, stationary);
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
                        : encasedItem(builder.base.importedModel,
                            builder.large, builder.brass, builder.base.textures, builder.base.allTexture);
            String itemParent = builder.itemBuilder.parentModel;
            if (builder.base == null && builder.modelOverrides().usesPrimaryItem()) {
                // The inherited one-argument API selects this parent for the item too.
                // Carry its explicit block texture slots across before item-specific overrides.
                item = customModel(builder, parent(itemParent));
                itemParent = "";
            }
            item = itemOverrides(item, builder.itemBuilder.modelJson, itemParent,
                    builder.itemBuilder.textureJson);
            put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }
    private static JsonObject customModel(CogwheelBuilder builder, JsonObject model) {
        // External parents resolve during resource loading. Only script-supplied slots
        // are added; unknown authored defaults are never read or replaced at startup.
        mergeTextures(model, builder.textures);
        return model;
    }
    static boolean customWheelModel(CogwheelBuilder builder) { return builder.modelOverrides().has("cogwheel"); }

    private static boolean customPartModels(CogwheelBuilder builder) {
        return customWheelModel(builder) || !builder.shaftless
                && (builder.modelOverrides().has("shaft") || builder.modelOverrides().has("shaft_half"));
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
        json.addProperty("loader", "forge:composite");
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
