package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedSmartFluidPipeBlock;

/** A Create smart pipe retaining its native filter and attachment controls. */
public final class SmartFluidPipeBuilder extends FluidDeviceBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body"), java.util.Set.of());
    public SmartFluidPipeBuilder(ResourceLocation id) {
        super(id);
        mapColor(MapColor.TERRACOTTA_YELLOW);
    }

    public String bodyTexture() { return bodyTexture(1); }

    public String bodyTexture(int index) {
        if (index < 1 || index > 3) throw new IllegalArgumentException("Smart pipe body texture index must be 1, 2 or 3");
        String nativeRole = "smart_pipe_" + index;
        if (textures.has(nativeRole)) return textures.get(nativeRole).getAsString();
        String role = "body_" + index;
        if (textures.has(role)) return textures.get(role).getAsString();
        return textureOrDefault("body", "create:block/smart_pipe_" + index);
    }

    public String pipeTexture() { return textureOrDefault("pipe", "create:block/pipes"); }

    @Override public SmartFluidPipeBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }

    public SmartFluidPipeBuilder model(String role, String path) {
        modelOverrides.set(role, path);
        return this;
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedSmartFluidPipeBlock(this);
    }

    @Override protected void registerDevice() { ModFluidDevices.add(this); }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { FluidDeviceAssets.generate(this, generator); }
}
