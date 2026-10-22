package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.CustomEncasedShaftBlock;
import net.quiwh.cbjs.content.ScriptedEncasedShaftBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Native andesite/brass companions for one scripted shaft family. */
final class ShaftEncasedBuilder extends BlockBuilder {
    final ShaftBuilder shaft;
    final boolean brass;
    private boolean additionalCreated;
    ShaftEncasedBuilder(ShaftBuilder shaft, boolean brass) {
        super(ResourceLocation.fromNamespaceAndPath(shaft.id.getNamespace(),
                (brass ? "brass" : "andesite") + "_encased_" + shaft.id.getPath()));
        this.shaft = shaft;
        this.brass = brass;
        CompanionInheritance.copy(shaft, this, false);
        notSolid = true;
        opaque = false;
        renderType = "cutout_mipped";
    }
    @Override public Block createObject() {
        return new ScriptedEncasedShaftBlock(this,
                () -> brass ? AllBlocks.BRASS_CASING.get() : AllBlocks.ANDESITE_CASING.get(), shaft);
    }
    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        additionalCreated = true;
        super.createAdditionalObjects();
        CustomCasingBlockEntities.addShaft(this);
        ModShafts.add(this);
        GearLimitDefaults.register(this, shaft.maxRpm, shaft.maxStress, shaft.independentRpmLimit, null);
    }
    @Override public void generateAssetJsons(AssetJsonGenerator generator) {
        String casing = "create:block/" + (brass ? "brass" : "andesite") + "_casing";
        String opening = "create:block/" + (brass ? "brass_gearbox" : "gearbox");
        GearAssets.put(generator, id, "models", "block/" + id.getPath() + "/block", CasingAssets.shaftModel(casing, opening, false));
        GearAssets.put(generator, id, "blockstates", id.getPath(), CasingAssets.axisState(id, true));
        if (itemBuilder != null) GearAssets.put(generator, id, "models/item", id.getPath(),
                CasingAssets.shaftItem(casing, opening, shaft));
    }
    @Override public void generateDataJsons(DataJsonGenerator generator) { shaft.generateLoot(generator, id); }
}
