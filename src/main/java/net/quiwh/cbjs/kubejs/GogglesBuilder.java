package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.quiwh.cbjs.content.ScriptedGogglesItem;

/** Native Create goggles with the ordinary KubeJS item customization surface. */
public final class GogglesBuilder extends ItemBuilder {
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
    @Override public void generateAssetJsons(AssetJsonGenerator generator) {
        if (modelJson != null || !parentModel.isEmpty()) super.generateAssetJsons(generator);
        else EquipmentItemAssets.goggles(this, generator);
    }
}
