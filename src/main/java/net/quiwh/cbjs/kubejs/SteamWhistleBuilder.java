package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.ScriptedSteamWhistleBlock;
import net.quiwh.cbjs.content.ScriptedSteamBlockEntities;

/** Native redstone whistle with a matching generated extension. */
public final class SteamWhistleBuilder extends FluidDeviceBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "whistle"), SteamAssets.WHISTLE_PARTS);
    public SteamWhistleExtensionBuilder extension;
    public SteamWhistleBuilder(ResourceLocation id) { super(id); }
    @Override public SteamWhistleBuilder replaceTexture(String role, String path) { super.replaceTexture(role, path); return this; }
    @Override public SteamWhistleBuilder model(String path) {
        super.model(ModelOverrides.validPath(path)); modelOverrides.primarySelected(); return this;
    }
    public SteamWhistleBuilder model(String role, String path) { modelOverrides.set(role, path); return this; }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedSteamWhistleBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        ScriptedSteamBlockEntities.addWhistle(this); ModSteamDevices.add(this);
        extension = new SteamWhistleExtensionBuilder(this);
        if (RegistryObjectStorage.BLOCK.objects.containsKey(extension.id) || RegistryObjectStorage.ITEM.objects.containsKey(extension.id))
            throw new IllegalArgumentException("Generated whistle extension ID already exists: " + extension.id);
        registry.add(net.minecraft.core.registries.Registries.BLOCK, extension); extension.createAdditionalObjects(registry);
    }
    @Override public void generateAssets(KubeAssetGenerator generator) { SteamAssets.whistle(this, generator); }
}
