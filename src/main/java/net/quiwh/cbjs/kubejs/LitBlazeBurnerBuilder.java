package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedLitBlazeBurnerBlock;

/** The fire/soul-fire cage is generated automatically and has no separate inventory item. */
final class LitBlazeBurnerBuilder extends CBJSBlockBuilder {
    final BlazeBurnerBuilder source;
    LitBlazeBurnerBuilder(BlazeBurnerBuilder source) {
        super(source.litId()); this.source = source;
        CompanionInheritance.copy(source, this, false);
        noItem(); notSolid = true; opaque = false; renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        tagBlock(net.minecraft.resources.ResourceLocation.parse("create:fan_processing_catalysts/haunting"));
        tagBlock(net.minecraft.resources.ResourceLocation.parse("create:fan_processing_catalysts/smoking"));
        tagBlock(net.minecraft.resources.ResourceLocation.parse("create:fan_transparent"));
        tagBlock(net.minecraft.resources.ResourceLocation.parse("create:passive_boiler_heaters"));
        ContraptionRules.inherit(this, source);
    }
    @Override public Block createObject() { return new ScriptedLitBlazeBurnerBlock(this, source); }
    @Override public void generateAssets(KubeAssetGenerator generator) { }
    @Override public void generateData(KubeDataGenerator generator) {
        generator.json(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_table/blocks/" + id.getPath()),
                source.familyLoot(true));
    }
}
