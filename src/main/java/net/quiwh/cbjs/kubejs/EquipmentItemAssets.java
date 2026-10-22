package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;

/** References native Create artwork; user-authored parent/model JSON remains authoritative. */
final class EquipmentItemAssets {
    static void goggles(ItemBuilder builder, AssetJsonGenerator generator) {
        var model = new JsonObject();
        model.addProperty("loader", "forge:separate_transforms");
        var icon = parent("create:item/goggles");
        icon.add("textures", builder.textureJson.deepCopy());
        var head = parent("create:block/goggles");
        var textures = builder.textureJson.deepCopy();
        alias(textures, "frame", "0");
        alias(textures, "lens", "1");
        alias(textures, "goggles", "2");
        head.add("textures", textures);
        model.add("base", icon);
        var perspectives = new JsonObject();
        perspectives.add("head", head);
        model.add("perspectives", perspectives);
        generator.json(AssetJsonGenerator.asItemModelLocation(builder.id), model);
    }
    static void wrench(ItemBuilder builder, AssetJsonGenerator generator) {
        var model = parent("create:item/wrench/item");
        model.addProperty("loader", "forge:composite");
        var textures = builder.textureJson.deepCopy();
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
        generator.json(AssetJsonGenerator.asItemModelLocation(builder.id), model);
    }
    private static void alias(JsonObject textures, String role, String nativeSlot) {
        if (textures.has(role)) textures.add(nativeSlot, textures.get(role));
    }
    private static JsonObject parent(String parent) {
        var model = new JsonObject(); model.addProperty("parent", parent); return model;
    }
    private EquipmentItemAssets() { }
}
