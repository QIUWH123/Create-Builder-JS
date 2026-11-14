package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedPumpBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** A mechanically driven Create pump with ordinary KubeJS block settings. */
public final class PumpBuilder extends FluidDeviceBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "pump"), java.util.Set.of("cog"));
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;

    public PumpBuilder(ResourceLocation id) {
        super(id);
        mapColor(MapColor.STONE);
    }

    public String bodyTexture() { return textureOrDefault("pump", "create:block/pump"); }
    public String cogTexture() { return textures.containsKey("cog") ? textures.get("cog") : bodyTexture(); }
    public String pipeTexture() { return textureOrDefault("pipe", "create:block/pipes"); }

    @Override public PumpBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }

    public PumpBuilder model(String role, String path) {
        modelOverrides.set("cogwheel".equals(role) ? "cog" : role, path);
        return this;
    }

    public PumpBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }

    public PumpBuilder noMaxRPM() {
        maxRpm = null;
        independentRpmLimit = true;
        return this;
    }

    public PumpBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }

    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedPumpBlock(this);
    }

    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        ModFluidDevices.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }

    @Override public void generateAssets(KubeAssetGenerator generator) { FluidDeviceAssets.generate(this, generator); }
}
