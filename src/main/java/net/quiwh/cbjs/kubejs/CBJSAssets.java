package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import java.util.Map;

final class CBJSAssets {
    static JsonObject json(Map<String, String> textures) {
        var result = new JsonObject();
        textures.forEach(result::addProperty);
        return result;
    }
    static JsonObject itemModel(ItemBuilder item) {
        JsonObject explicit = item instanceof CBJSBlockItemBuilder builder ? builder.modelJson
                : item instanceof CBJSItemBuilder builder ? builder.modelJson : null;
        if (explicit != null) return explicit;
        if (item.modelGenerator == null) return null;
        var model = new dev.latvian.mods.kubejs.client.ModelGenerator();
        if (item.parentModel != null) model.parent(item.parentModel);
        model.textures(item.textures);
        item.modelGenerator.accept(model);
        return model.toJson();
    }
    static JsonObject itemTextures(ItemBuilder item) {
        var result = json(item.textures);
        if (item instanceof CBJSBlockItemBuilder builder && builder.textureJson != null)
            builder.textureJson.entrySet().forEach(entry -> result.add(entry.getKey(), entry.getValue()));
        return result;
    }
    static String parent(ItemBuilder item) { return item.parentModel == null ? null : item.parentModel.toString(); }
    private CBJSAssets() {}
}
