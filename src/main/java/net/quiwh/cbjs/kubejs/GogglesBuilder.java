package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.quiwh.cbjs.content.ScriptedGogglesItem;

/** Native Create goggles with the ordinary KubeJS item customization surface. */
public final class GogglesBuilder extends CBJSItemBuilder {
    public GogglesBuilder(ResourceLocation id) {
        super(id);
        maxStackSize(1);
        tag(ResourceLocation.parse("curios:head"));
    }
    public GogglesBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }
    @Override public Item createObject() { return new ScriptedGogglesItem(this); }
    @Override public void generateAssets(KubeAssetGenerator generator) {
        if (modelJson != null || parentModel != null || modelGenerator != null) super.generateAssets(generator);
        else EquipmentItemAssets.goggles(this, generator);
    }
}
