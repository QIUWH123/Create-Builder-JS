package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedFluidValveBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** A Create fluid valve with normal script block settings and optional kinetic limits. */
public final class FluidValveBuilder extends FluidDeviceBuilder {
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;

    public FluidValveBuilder(ResourceLocation id) {
        super(id);
        renderType = "cutout_mipped";
    }

    public String bodyTexture() { return textureOrDefault("fluid_valve", "create:block/fluid_valve"); }
    public String pipeTexture() { return textureOrDefault("pipe", "create:block/pipes"); }

    public FluidValveBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }

    public FluidValveBuilder noMaxRPM() {
        maxRpm = null;
        independentRpmLimit = true;
        return this;
    }

    public FluidValveBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }

    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedFluidValveBlock(this);
    }

    @Override protected void registerDevice() {
        ModFluidDevices.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { FluidDeviceAssets.generate(this, generator); }
}
