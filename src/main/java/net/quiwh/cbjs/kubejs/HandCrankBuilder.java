package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.*;

/** Native ten-tick hand crank, hunger cost, waterlogging and reverse rotation. */
public final class HandCrankBuilder extends NativeKineticMachineBuilder {
    public HandCrankBuilder(ResourceLocation id) {
        super(id, "hand_crank", Set.of("base", "handle"));
        hardness = 2;
        resistance = 2;
        soundType = SoundType.WOOD;
        requiresTool = false;
        mapColor(MapColor.PODZOL);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        tagBlock(ResourceLocation.parse("create:brittle"));
    }
    @Override protected boolean primaryPart(String role) { return role.equals("base"); }
    @Override protected boolean primaryStaticModel() { return false; }
    @Override protected Map<String, String> nativeParts() { return Map.of("base", "create:block/hand_crank/block", "handle", "create:block/hand_crank/handle"); }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedHandCrankBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { ScriptedGeneratorBlockEntities.add(this); ModNativeGenerators.add(this); registerKineticDefaults(); }
}
