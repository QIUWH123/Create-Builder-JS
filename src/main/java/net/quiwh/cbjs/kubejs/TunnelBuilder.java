package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native belt tunnel, optionally retaining brass filtering and group distribution. */
public final class TunnelBuilder extends NativeMachineBuilder {
    public final boolean brass;
    public TunnelBuilder(ResourceLocation id, boolean brass) {
        super(id, brass ? "brass_tunnel" : "andesite_tunnel", java.util.Set.of("flap"));
        this.brass = brass; renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        mapColor(brass ? MapColor.TERRACOTTA_YELLOW : MapColor.STONE);
        getOrCreateItemBuilder();
    }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof TunnelItemBuilder)) {
            itemBuilder = new TunnelItemBuilder(id); ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        }
        return (BlockItemBuilder) itemBuilder;
    }
    /** Native brass tunnel CT needs a connected atlas matching the selected top texture. */
    public String topTexture(boolean connected) {
        String name = "brass_tunnel_top" + (connected ? "_connected" : "");
        String nativeTexture = "create:block/tunnel/" + name;
        String selected = allTexture == null ? nativeTexture : allTexture;
        for (String role : new String[]{"all", "body", name, nativeTexture, connected ? "top_connected" : "top"})
            if (textures.containsKey(role)) selected = textures.get(role);
        return selected;
    }
    @Override protected java.util.Map<String, String> nativeParts() { return java.util.Map.of("flap", "create:block/belt_tunnel/flap"); }
    @Override public Block createObject() { validateBlockEntity(); return brass ? new ScriptedBrassTunnelBlock(this) : new ScriptedTunnelBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedLogisticsBlockEntities.addTunnel(this, brass); ModLogistics.add(this); }
    private static final class TunnelItemBuilder extends CBJSBlockItemBuilder {
        TunnelItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return new ScriptedTunnelItem(blockBuilder.get(), createItemProperties()); }
    }
}
