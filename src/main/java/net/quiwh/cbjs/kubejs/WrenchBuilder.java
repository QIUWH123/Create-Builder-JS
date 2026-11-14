package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.quiwh.cbjs.content.ScriptedWrenchItem;

/** Native Create wrench with custom inventory models and ordinary KubeJS item settings. */
public final class WrenchBuilder extends CBJSItemBuilder {
    public WrenchBuilder(ResourceLocation id) {
        super(id);
        maxStackSize(1);
        tag(ResourceLocation.parse("c:tools/wrench"));
    }
    public WrenchBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }
    @Override public Item createObject() { return new ScriptedWrenchItem(this); }
    @Override public void generateAssets(KubeAssetGenerator generator) {
        if (modelJson != null || parentModel != null || modelGenerator != null) super.generateAssets(generator);
        else EquipmentItemAssets.wrench(this, generator);
    }
}
