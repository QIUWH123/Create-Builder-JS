package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;

/** References native Create artwork; user-authored parent/model JSON remains authoritative. */
final class EquipmentItemAssets {
    static void goggles(ItemBuilder builder, KubeAssetGenerator generator) {
        var model = new JsonObject();
        model.addProperty("loader", "neoforge:separate_transforms");
        model.addProperty("gui_light", "front");
        var icon = parent("create:item/goggles");
        icon.add("textures", CBJSAssets.itemTextures(builder));
        var head = parent("create:block/goggles");
        var textures = CBJSAssets.itemTextures(builder);
        alias(textures, "frame", "0");
        alias(textures, "lens", "1");
        alias(textures, "goggles", "2");
        head.add("textures", textures);
        model.add("base", icon);
        var perspectives = new JsonObject();
        perspectives.add("head", head);
        model.add("perspectives", perspectives);
        generator.json(builder.id.withPath(path -> "models/item/" + path), model);
    }
    static void wrench(ItemBuilder builder, KubeAssetGenerator generator) {
        var model = parent("create:item/wrench/item");
        model.addProperty("loader", "neoforge:composite");
        var textures = CBJSAssets.itemTextures(builder);
        if (textures.has("wrench")) textures.add("5", textures.get("wrench"));
        else if (textures.has("layer0")) textures.add("5", textures.get("layer0"));
        if (textures.has("5") && !textures.has("particle")) textures.add("particle", textures.get("5"));
        var children = new JsonObject();
        for (String part : new String[]{"body", "gear"}) {
            var child = parent("create:item/wrench/" + (part.equals("body") ? "item" : "gear"));
            child.add("textures", textures.deepCopy());
            children.add(part, child);
        }
        model.add("children", children);
        generator.json(builder.id.withPath(path -> "models/item/" + path), model);
    }
    private static void alias(JsonObject textures, String role, String nativeSlot) {
        if (textures.has(role)) textures.add(nativeSlot, textures.get(role));
    }
    private static JsonObject parent(String parent) {
        var model = new JsonObject(); model.addProperty("parent", parent); return model;
    }
    private EquipmentItemAssets() { }
}
