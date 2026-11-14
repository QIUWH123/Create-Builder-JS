package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** A native wall funnel and its material-preserving belt-mounted companion. */
public final class FunnelBuilder extends NativeMachineBuilder {
    public final boolean brass;
    public BeltFunnelBuilder belt;
    public FunnelBuilder(ResourceLocation id, boolean brass) {
        super(id, brass ? "brass_funnel" : "andesite_funnel", java.util.Set.of("flap", "belt_flap", "belt"));
        this.brass = brass; renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        hardness = brass ? 3 : 1.5F;
        soundType = brass ? SoundType.METAL : SoundType.STONE;
        mapColor(brass ? MapColor.TERRACOTTA_YELLOW : MapColor.STONE);
        getOrCreateItemBuilder();
        tagBlock(ResourceLocation.parse("create:safe_nbt"));
    }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof FunnelItemBuilder)) {
            itemBuilder = new FunnelItemBuilder(id); ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
            itemBuilder.tag(new ResourceLocation[]{ResourceLocation.parse("create:contraption_controlled")});
        }
        return (BlockItemBuilder) itemBuilder;
    }
    public ResourceLocation beltId() { return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_belt"); }
    @Override protected java.util.Map<String, String> nativeParts() {
        return java.util.Map.of("flap", "create:block/funnel/flap", "belt_flap", "create:block/belt_funnel/flap");
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedFunnelBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (RegistryObjectStorage.BLOCK.objects.containsKey(beltId()) || RegistryObjectStorage.ITEM.objects.containsKey(beltId()))
            throw new IllegalArgumentException("Generated belt funnel ID already registered: " + beltId());
        belt = new BeltFunnelBuilder(this); registry.add(net.minecraft.core.registries.Registries.BLOCK, belt); belt.createAdditionalObjects(registry);
        ScriptedLogisticsBlockEntities.addFunnel(this); ModLogistics.add(this);
    }
    private static final class FunnelItemBuilder extends CBJSBlockItemBuilder {
        FunnelItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return new com.simibubi.create.content.logistics.funnel.FunnelItem(blockBuilder.get(), createItemProperties()); }
    }
}
