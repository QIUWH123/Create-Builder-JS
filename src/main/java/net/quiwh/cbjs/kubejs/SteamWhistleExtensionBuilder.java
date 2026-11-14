package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedSteamWhistleExtensionBlock;

public final class SteamWhistleExtensionBuilder extends CBJSBlockBuilder {
    public final SteamWhistleBuilder whistle;
    SteamWhistleExtensionBuilder(SteamWhistleBuilder whistle) {
        super(ResourceLocation.fromNamespaceAndPath(whistle.id.getNamespace(), whistle.id.getPath() + "_extension"));
        this.whistle = whistle; CompanionInheritance.copy(whistle, this, false); noItem(); noDrops();
    }
    @Override public Block createObject() { return new ScriptedSteamWhistleExtensionBlock(this); }
    @Override public void generateAssets(KubeAssetGenerator generator) { SteamAssets.extension(this, generator); }
    @Override public void generateData(KubeDataGenerator generator) {
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_table/blocks/" + id.getPath()), GearAssets.defaultLoot(null));
    }
}
