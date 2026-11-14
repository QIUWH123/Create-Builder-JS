package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/** Retains explicit JSON model input on generated block items. */
public class CBJSBlockItemBuilder extends dev.latvian.mods.kubejs.block.BlockItemBuilder {
    public JsonObject modelJson;
    public JsonObject textureJson;
    public CBJSBlockItemBuilder(ResourceLocation id) { super(id); }
}
