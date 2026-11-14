package net.quiwh.cbjs.kubejs;

import com.google.gson.*;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Generates independent aliases from installed native artwork, preserving state transforms. */
public final class NativeMachineAssets {
    public static ResourceLocation partModelId(ResourceLocation id, String role) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/" + role);
    }

    public static void generate(NativeMachineBuilder builder, KubeAssetGenerator generator) {
        var models = new HashMap<String, String>();
        JsonObject states = ScriptModelResources.loadBlockstate(builder.nativeBlockId());
        // Standard aliases remain available to custom blockstates, just like moving
        // parts. An explicit state replaces the state JSON without rewriting its paths.
        repoint(states, builder, generator, models);
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(),
                builder.blockstateJson == null ? states : builder.blockstateJson.deepCopy());
        for (var part : builder.nativeParts().entrySet()) {
            var json = builder.modelOverrides.has(part.getKey())
                    ? builder.modelOverrides.partModel(part.getKey(), part.getValue())
                    : builder.primaryPart(part.getKey()) && builder.modelOverrides.customPrimary()
                    ? builder.modelOverrides.primaryModel(part.getValue())
                    : ScriptModelResources.resolved(ResourceLocation.parse(part.getValue()));
            applyTextures(builder, json, part.getKey());
            GearAssets.put(generator, builder.id, "models", partModelId(builder.id, part.getKey()).getPath(), json);
        }
        if (builder.itemBuilder != null) {
            boolean primary = builder.modelOverrides.usesPrimaryItem();
            var item = primary ? GearAssets.parent(builder.model)
                    : ScriptModelResources.resolved(ResourceLocation.fromNamespaceAndPath("create", "item/" + builder.nativeBlockId().getPath()));
            applyTextures(builder, item, "item");
            item = GearAssets.itemOverrides(item, CBJSAssets.itemModel(builder.itemBuilder),
                    primary ? null : CBJSAssets.parent(builder.itemBuilder), CBJSAssets.itemTextures(builder.itemBuilder));
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }

    private static void repoint(JsonElement value, NativeMachineBuilder builder, KubeAssetGenerator generator, Map<String,String> models) {
        if (value.isJsonArray()) {
            for (var child : value.getAsJsonArray()) repoint(child, builder, generator, models);
        } else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("model") && object.get("model").isJsonPrimitive()) {
                String source = object.get("model").getAsString();
                String target = models.get(source);
                if (target == null) {
                    var sourceId = ResourceLocation.parse(source);
                    boolean customBody = builder.primaryStaticModel() && builder.modelOverrides.customPrimary();
                    var targetId = customBody ? partModelId(builder.id, "block")
                            : partModelId(builder.id, "native/" + sourceId.getNamespace() + "/" + sourceId.getPath());
                    var model = customBody ? builder.modelOverrides.primaryModel(source)
                            : ScriptModelResources.resolved(sourceId);
                    applyTextures(builder, model, "body");
                    GearAssets.put(generator, builder.id, "models", targetId.getPath(), model);
                    target = targetId.toString(); models.put(source, target);
                }
                object.addProperty("model", target);
            }
            for (var entry : object.entrySet()) if (!entry.getKey().equals("model")) repoint(entry.getValue(), builder, generator, models);
        }
    }

    private static void applyTextures(NativeMachineBuilder builder, JsonObject model, String role) {
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        for (var slot : slots.entrySet()) {
            String original = slot.getValue().getAsString();
            // References retain their relationship to the actual material slots.
            if (original.startsWith("#")) continue;
            String replacement = builder.allTexture;
            if (builder.textures.containsKey("all")) replacement = builder.textures.get("all");
            if (builder.textures.containsKey(role)) replacement = builder.textures.get(role);
            ResourceLocation textureId = ResourceLocation.parse(original);
            String texture = textureId.getPath();
            String basename = texture.substring(texture.lastIndexOf('/') + 1);
            if (builder.textures.containsKey(basename)) replacement = builder.textures.get(basename);
            // Native models may omit the minecraft namespace; script selectors use
            // canonical resource IDs for both namespaced and shorthand references.
            if (builder.textures.containsKey(textureId.toString())) replacement = builder.textures.get(textureId.toString());
            if (builder.textures.containsKey(original)) replacement = builder.textures.get(original);
            if (replacement != null) slot.setValue(new JsonPrimitive(replacement));
        }
        // Raw slots have final precedence, including for an externally supplied parent.
        for (var entry : builder.textures.entrySet()) slots.addProperty(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
        // Forge composite models can own independent child material dictionaries.
        if (model.has("children")) for (var child : model.getAsJsonObject("children").entrySet())
            if (child.getValue().isJsonObject()) applyTextures(builder, child.getValue().getAsJsonObject(), role);
    }
    private NativeMachineAssets() { }
}
