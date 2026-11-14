package net.quiwh.cbjs.content;

import com.simibubi.create.content.contraptions.bearing.*;
import com.simibubi.create.content.kinetics.crank.*;
import com.simibubi.create.content.kinetics.motor.*;
import com.simibubi.create.content.kinetics.waterwheel.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;
import net.quiwh.cbjs.kubejs.*;

/** Native generator entities with independent valid-block sets. */
public final class ScriptedGeneratorBlockEntities {
    private static final BlockEntityBuilder<WaterWheelBlockEntity, CreateRegistrate> WATER_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_water_wheel", WaterWheelBlockEntity::new)
            .visual(() -> (context, entity, pt) -> new NativeGeneratorClientModels.WheelVisual<>(context, entity, false, pt), false)
            .renderer(() -> NativeGeneratorClientModels::smallWheelRenderer);
    public static final BlockEntityEntry<WaterWheelBlockEntity> WATER_WHEEL = WATER_BUILDER.register();
    private static final BlockEntityBuilder<LargeWaterWheelBlockEntity, CreateRegistrate> LARGE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_large_water_wheel", LargeWaterWheelBlockEntity::new)
            .visual(() -> (context, entity, pt) -> new NativeGeneratorClientModels.WheelVisual<>(context, entity, true, pt), false)
            .renderer(() -> NativeGeneratorClientModels::largeWheelRenderer);
    public static final BlockEntityEntry<LargeWaterWheelBlockEntity> LARGE_WATER_WHEEL = LARGE_BUILDER.register();
    private static final BlockEntityBuilder<WindmillBearingBlockEntity, CreateRegistrate> WINDMILL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_windmill_bearing", WindmillBearingBlockEntity::new)
            .visual(() -> BearingVisual::new)
            .renderer(() -> BearingRenderer::new);
    public static final BlockEntityEntry<WindmillBearingBlockEntity> WINDMILL = WINDMILL_BUILDER.register();
    private static final BlockEntityBuilder<HandCrankBlockEntity, CreateRegistrate> CRANK_BUILDER = CBJS.REGISTRATE
            .<HandCrankBlockEntity>blockEntity("custom_hand_crank", ScriptedHandCrankBlockEntity::new)
            .visual(() -> HandCrankVisual::new)
            .renderer(() -> NativeGeneratorClientModels.CrankRenderer::new);
    public static final BlockEntityEntry<HandCrankBlockEntity> CRANK = CRANK_BUILDER.register();
    private static final BlockEntityBuilder<CreativeMotorBlockEntity, CreateRegistrate> MOTOR_BUILDER = CBJS.REGISTRATE
            .<CreativeMotorBlockEntity>blockEntity("custom_creative_motor", ScriptedCreativeMotorBlockEntity::new)
            .visual(() -> NativeGeneratorClientModels.MotorVisual::new, false)
            .renderer(() -> NativeGeneratorClientModels.MotorRenderer::new);
    public static final BlockEntityEntry<CreativeMotorBlockEntity> MOTOR = MOTOR_BUILDER.register();
    public static void add(WaterWheelBuilder builder) {
        if (builder.large) ScriptedBlockEntityValidity.add(LARGE_WATER_WHEEL::get, builder::get);
        else ScriptedBlockEntityValidity.add(WATER_WHEEL::get, builder::get);
    }
    public static void add(WindmillBearingBuilder builder) {
        ScriptedBlockEntityValidity.add(WINDMILL::get, builder::get);
    }
    public static void add(HandCrankBuilder builder) {
        ScriptedBlockEntityValidity.add(CRANK::get, builder::get);
    }
    public static void add(CreativeMotorBuilder builder) {
        ScriptedBlockEntityValidity.add(MOTOR::get, builder::get);
    }
    public static void register() { }
    private ScriptedGeneratorBlockEntities() { }
}
