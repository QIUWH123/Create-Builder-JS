package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.mechanicalArm.*;
import com.simibubi.create.content.kinetics.flywheel.*;
import com.simibubi.create.content.kinetics.gauge.*;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.ArmGaugeClientModels;
import net.quiwh.cbjs.kubejs.GaugeBuilder;
import java.util.function.Supplier;

public final class ScriptedArmGaugeBlockEntities {
    private static final BlockEntityBuilder<ArmBlockEntity, CreateRegistrate> ARM_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_mechanical_arm", ArmBlockEntity::new).visual(() -> ArmVisual::new, false)
            .renderer(() -> ArmRenderer::new);
    public static final BlockEntityEntry<ArmBlockEntity> MECHANICAL_ARM = ARM_BUILDER.register();
    private static final BlockEntityBuilder<FlywheelBlockEntity, CreateRegistrate> FLYWHEEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_flywheel", FlywheelBlockEntity::new).visual(() -> FlywheelVisual::new, false)
            .renderer(() -> ArmGaugeClientModels.FlywheelRenderer::new);
    public static final BlockEntityEntry<FlywheelBlockEntity> FLYWHEEL = FLYWHEEL_BUILDER.register();
    private static final BlockEntityBuilder<SpeedGaugeBlockEntity, CreateRegistrate> SPEED_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_speedometer", SpeedGaugeBlockEntity::new).visual(() -> ArmGaugeClientModels.SpeedVisual::new, false)
            .renderer(() -> ArmGaugeClientModels.GaugeRenderer::speed);
    public static final BlockEntityEntry<SpeedGaugeBlockEntity> SPEEDOMETER = SPEED_BUILDER.register();
    private static final BlockEntityBuilder<StressGaugeBlockEntity, CreateRegistrate> STRESS_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_stressometer", StressGaugeBlockEntity::new).visual(() -> ArmGaugeClientModels.StressVisual::new, false)
            .renderer(() -> ArmGaugeClientModels.GaugeRenderer::stress);
    public static final BlockEntityEntry<StressGaugeBlockEntity> STRESSOMETER = STRESS_BUILDER.register();
    public static void addMechanicalArm(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(MECHANICAL_ARM::get, block);
    }
    public static void addFlywheel(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(FLYWHEEL::get, block);
    }
    public static void addGauge(GaugeBuilder block) {
        if (block.stress) ScriptedBlockEntityValidity.add(STRESSOMETER::get, block::get);
        else ScriptedBlockEntityValidity.add(SPEEDOMETER::get, block::get);
    }
    public static void register() { }
    private ScriptedArmGaugeBlockEntities() { }
}
