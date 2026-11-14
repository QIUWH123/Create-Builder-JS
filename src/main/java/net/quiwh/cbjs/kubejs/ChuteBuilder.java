package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native gravity/fan transport, with the smart variant retaining filtering and redstone. */
public final class ChuteBuilder extends NativeMachineBuilder {
    public final boolean smart;
    public ChuteBuilder(ResourceLocation id, boolean smart) {
        super(id, smart ? "smart_chute" : "chute", java.util.Set.of());
        this.smart = smart; renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        hardness = 3;
        soundType = SoundType.NETHERITE_BLOCK;
        mapColor(MapColor.COLOR_GRAY);
        getOrCreateItemBuilder();
    }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof ChuteItemBuilder)) {
            itemBuilder = new ChuteItemBuilder(id); ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        }
        return (BlockItemBuilder) itemBuilder;
    }
    @Override public Block createObject() { validateBlockEntity(); return smart ? new ScriptedSmartChuteBlock(this) : new ScriptedChuteBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedLogisticsBlockEntities.addChute(this, smart); }
    private static final class ChuteItemBuilder extends CBJSBlockItemBuilder {
        ChuteItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() {
            return ((ChuteBuilder) blockBuilder).smart
                    ? new net.minecraft.world.item.BlockItem(blockBuilder.get(), createItemProperties())
                    : new com.simibubi.create.content.logistics.chute.ChuteItem(blockBuilder.get(), createItemProperties());
        }
    }
}
