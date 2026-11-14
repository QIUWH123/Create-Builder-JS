package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedGaugeBlock;
import net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities;

/** Native speed/stress meters, including comparator, goggles and computer behavior. */
public final class GaugeBuilder extends NativeKineticMachineBuilder {
    public final boolean stress;
    public GaugeBuilder(ResourceLocation id, boolean stress) {
        super(id, stress ? "stressometer" : "speedometer", Set.of("head", "dial", "shaft")); this.stress = stress;
        hardness = 2;
        resistance = 2;
        soundType = SoundType.WOOD;
        requiresTool = false;
        mapColor(MapColor.PODZOL);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedGaugeBlock(this); }
    @Override protected Map<String, String> nativeParts() { return Map.of("head", "create:block/gauge/" + (stress ? "stressometer" : "speedometer") + "/head",
            "dial", "create:block/gauge/dial", "shaft", "create:block/shaft"); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { registerKineticDefaults(); ScriptedArmGaugeBlockEntities.addGauge(this); ModArmGaugeMachines.add(this); }
}
