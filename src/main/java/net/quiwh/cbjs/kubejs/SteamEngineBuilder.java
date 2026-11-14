package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedSteamEngineBlock;
import net.quiwh.cbjs.content.ScriptedSteamBlockEntities;

/** Native steam engine; power allocation and RPM remain Create boiler mechanics. */
public final class SteamEngineBuilder extends FluidDeviceBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "engine"), java.util.Set.of("piston", "linkage", "connector"));
    public SteamEngineBuilder(ResourceLocation id) { super(id); }
    @Override public SteamEngineBuilder replaceTexture(String role, String path) { super.replaceTexture(role, path); return this; }
    @Override public SteamEngineBuilder model(String path) {
        super.model(ModelOverrides.validPath(path)); modelOverrides.primarySelected(); return this;
    }
    public SteamEngineBuilder model(String role, String path) { modelOverrides.set(role, path); return this; }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedSteamEngineBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedSteamBlockEntities.addEngine(this); ModSteamDevices.add(this); }
    @Override public void generateAssets(KubeAssetGenerator generator) { SteamAssets.engine(this, generator); }
}
