package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedPoweredShaftBlock;
import net.quiwh.cbjs.content.ScriptedSteamBlockEntities;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Internal companion; breaking or cloning resolves to the original material shaft. */
public final class ShaftPoweredBuilder extends CBJSBlockBuilder {
    public final ShaftBuilder shaft;
    private boolean additionalCreated;
    ShaftPoweredBuilder(ShaftBuilder shaft) {
        super(ResourceLocation.fromNamespaceAndPath(shaft.id.getNamespace(), "powered_" + shaft.id.getPath()));
        this.shaft = shaft; CompanionInheritance.copy(shaft, this, false); noItem();
    }
    @Override public Block createObject() { return new ScriptedPoweredShaftBlock(this); }
    @Override public void createAdditionalObjects(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (additionalCreated) return;
        additionalCreated = true; super.createAdditionalObjects(registry); ScriptedSteamBlockEntities.addShaft(this);
        GearLimitDefaults.register(this, shaft.maxRpm, shaft.maxStress, shaft.independentRpmLimit, null);
    }
    @Override public void generateAssets(KubeAssetGenerator generator) {
        GearAssets.put(generator, id, "models", "block/" + id.getPath() + "/block", shaft.shaftModel());
        GearAssets.put(generator, id, "blockstates", id.getPath(), CasingAssets.axisState(id, true));
    }
    @Override public void generateData(KubeDataGenerator generator) { shaft.generateLoot(generator, id); }
}
