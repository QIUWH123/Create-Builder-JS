package net.quiwh.cbjs.kubejs;

import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedMechanicalArmBlock;
import net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities;

/** Native Create mechanics with registration-specific models and kinetic limits. */
public final class MechanicalArmBuilder extends NativeKineticMachineBuilder {
    public MechanicalArmBuilder(ResourceLocation id) {
        super(id, "mechanical_arm", Set.of("cog","base","lower_body","upper_body","claw","claw_goggles","grip_upper","grip_lower"));
        hardness = 3;
        soundType = SoundType.METAL;
        mapColor(MapColor.TERRACOTTA_YELLOW);
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        getOrCreateItemBuilder();
    }
    @Override protected dev.latvian.mods.kubejs.block.BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof MechanicalArmItemBuilder)) {
            itemBuilder = new MechanicalArmItemBuilder(id); ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        }
        return (dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder;
    }
    private static final class MechanicalArmItemBuilder extends dev.latvian.mods.kubejs.block.BlockItemBuilder {
        MechanicalArmItemBuilder(ResourceLocation id) { super(id); }
        @Override public net.minecraft.world.item.Item createObject() {
            return new com.simibubi.create.content.kinetics.mechanicalArm.ArmItem(blockBuilder.get(), createItemProperties());
        }
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedMechanicalArmBlock(this); }
    @Override protected Map<String, String> nativeParts() { return Map.ofEntries(Map.entry("cog", "create:block/mechanical_arm/cog"),
                Map.entry("base", "create:block/mechanical_arm/base"),
                Map.entry("lower_body", "create:block/mechanical_arm/lower_body"),
                Map.entry("upper_body", "create:block/mechanical_arm/upper_body"),
                Map.entry("claw", "create:block/mechanical_arm/claw_base"),
                Map.entry("claw_goggles", "create:block/mechanical_arm/claw_base_goggles"),
                Map.entry("grip_upper", "create:block/mechanical_arm/upper_claw_grip"),
                Map.entry("grip_lower", "create:block/mechanical_arm/lower_claw_grip")); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) { registerKineticDefaults(); ScriptedArmGaugeBlockEntities.addMechanicalArm(this); ModArmGaugeMachines.add(this); }
}
