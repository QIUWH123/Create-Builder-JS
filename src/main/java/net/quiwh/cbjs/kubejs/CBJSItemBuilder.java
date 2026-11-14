package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;

/** Explicit JSON inventory models remain available on CBJS equipment. */
public class CBJSItemBuilder extends dev.latvian.mods.kubejs.item.ItemBuilder {
    public JsonObject modelJson;
    public CBJSItemBuilder(ResourceLocation id) { super(id); }
    @Override public CBJSItemBuilder texture(String path) {
        super.texture(path);
        textures.put("layer0", path);
        return this;
    }
    public CBJSItemBuilder texture(String role, String path) { super.texture(new String[]{role}, path); return this; }
    @Override public CBJSItemBuilder tag(ResourceLocation... tags) { super.tag(tags); return this; }
    @Override public void generateAssets(KubeAssetGenerator generator) {
        if (modelJson != null) generator.json(id.withPath(path -> "models/item/" + path), modelJson.deepCopy());
        else super.generateAssets(generator);
    }
}
