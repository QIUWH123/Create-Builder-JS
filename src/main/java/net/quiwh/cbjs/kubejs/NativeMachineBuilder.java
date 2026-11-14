package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;

/** Shared native machinery metadata; mechanics and registration belong to each family. */
public abstract class NativeMachineBuilder extends FluidDeviceBuilder {
    public final ModelOverrides modelOverrides;
    private final ResourceLocation nativeBlockId;

    protected NativeMachineBuilder(ResourceLocation id, String nativeName, Set<String> movingPartNames) {
        super(id);
        // Create's SharedProperties.stone is the baseline; each family selects its material below.
        hardness = 1.5F;
        resistance = 6;
        soundType = SoundType.STONE;
        requiresTool = true;
        mapColor(MapColor.STONE);
        nativeBlockId = ResourceLocation.fromNamespaceAndPath("create", nativeName);
        var bodyRoles = new java.util.HashSet<>(Set.of("block", "body"));
        bodyRoles.add(nativeName);
        modelOverrides = new ModelOverrides(this, bodyRoles, movingPartNames);
        renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
    }

    public final ResourceLocation nativeBlockId() { return nativeBlockId; }
    @Override public NativeMachineBuilder replaceTexture(String role, String path) {
        super.replaceTexture(role, path); return this;
    }
    @Override public NativeMachineBuilder textureAll(String path) { super.textureAll(path); return this; }
    @Override public NativeMachineBuilder model(String path) {
        super.model(ModelOverrides.validPath(path)); modelOverrides.primarySelected(); return this;
    }
    public NativeMachineBuilder model(String role, String path) { modelOverrides.set(role, path); return this; }

    /** Moving aliases exported by this family, with native source geometry. */
    protected Map<String, String> nativeParts() { return Map.of(); }
    /** Families whose principal mesh is animated can route the main model to that mesh. */
    protected boolean primaryPart(String role) { return false; }
    /** Disable a second stationary copy when model(path) supplies animated geometry. */
    protected boolean primaryStaticModel() { return true; }
    @Override public void generateAssets(KubeAssetGenerator generator) { NativeMachineAssets.generate(this, generator); }
}
