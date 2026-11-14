package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedFlywheelBlock;
import net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities;

/** Native Create mechanics with registration-specific models and kinetic limits. */
public final class FlywheelBuilder extends NativeKineticMachineBuilder {
    public FlywheelBuilder(ResourceLocation id) {
        super(id, "flywheel", Set.of("wheel","shaft"));
        hardness = 3;
        soundType = SoundType.METAL;
        mapColor(MapColor.TERRACOTTA_YELLOW);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
    }
    @Override protected boolean primaryPart(String role) { return role.equals("wheel"); }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedFlywheelBlock(this); }
    @Override protected Map<String, String> nativeParts() { return Map.ofEntries(Map.entry("wheel", "create:block/flywheel/block"),
                Map.entry("shaft", "create:block/shaft")); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { registerKineticDefaults(); ScriptedArmGaugeBlockEntities.addFlywheel(this); ModArmGaugeMachines.add(this); }
}
