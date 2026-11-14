package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.*;

/** Internal placed companion; the declared funnel item places both native forms. */
public final class BeltFunnelBuilder extends NativeMachineBuilder {
    public final FunnelBuilder funnel;
    BeltFunnelBuilder(FunnelBuilder funnel) {
        super(funnel.beltId(), funnel.brass ? "brass_belt_funnel" : "andesite_belt_funnel", java.util.Set.of());
        this.funnel = funnel; CompanionInheritance.copy(funnel, this, false);
        textures.entrySet().clear(); funnel.textures.entrySet().forEach(e -> textures.put(e.getKey(), e.getValue())); allTexture = funnel.allTexture;
        renderType = funnel.renderType;
        if (funnel.modelOverrides.has("belt")) model = funnel.modelOverrides.get("belt");
        noItem();
        tagBlock(ResourceLocation.parse("create:safe_nbt"));
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedBeltFunnelBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedLogisticsBlockEntities.addFunnel(this); }
    @Override public void generateData(KubeDataGenerator generator) {
        funnel.generateData(new KubeDataGenerator() {
            @Override public dev.latvian.mods.kubejs.util.RegistryAccessContainer getRegistries() { return generator.getRegistries(); }
            @Override public void add(GeneratedData data) {
                var destination = id.withPath(path -> "loot_table/blocks/" + path + ".json");
                generator.add(new GeneratedData(destination, data.data()));
            }
            @Override public GeneratedData getGenerated(ResourceLocation location) { return generator.getGenerated(location); }
            @Override public <R, T> void dataMap(net.neoforged.neoforge.registries.datamaps.DataMapType<R, T> type,
                    java.util.function.Consumer<dev.latvian.mods.kubejs.script.data.VirtualDataMapFile<R, T>> callback) {
                generator.dataMap(type, callback);
            }
        });
    }
}
