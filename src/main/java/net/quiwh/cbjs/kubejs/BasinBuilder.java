package net.quiwh.cbjs.kubejs;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native basin inventories, fluid tanks, recipe filter and processing/output behavior. */
public final class BasinBuilder extends NativeMachineBuilder {
    public BasinBuilder(ResourceLocation id) {
        super(id, "basin", java.util.Set.of());
        soundType = SoundType.NETHERITE_BLOCK;
        mapColor(MapColor.COLOR_GRAY);
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedBasinBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedLogisticsBlockEntities.addBasin(this); ModLogistics.add(this); }
}
