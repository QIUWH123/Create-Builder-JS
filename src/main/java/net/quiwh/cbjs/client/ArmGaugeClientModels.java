package net.quiwh.cbjs.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.gauge.*;
import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kubejs.*;

/** Part identities shared by native fallback and instanced renderers. */
public final class ArmGaugeClientModels {
    private static final Map<ResourceLocation, Map<PartialModel, PartialModel>> MODELS = new ConcurrentHashMap<>();
    public static void declare(NativeKineticMachineBuilder builder) {
        Map<PartialModel, PartialModel> parts = new HashMap<>();
        if (builder instanceof MechanicalArmBuilder) {
            part(parts, builder, AllPartialModels.ARM_COG, "cog"); part(parts, builder, AllPartialModels.ARM_BASE, "base");
            part(parts, builder, AllPartialModels.ARM_LOWER_BODY, "lower_body"); part(parts, builder, AllPartialModels.ARM_UPPER_BODY, "upper_body");
            part(parts, builder, AllPartialModels.ARM_CLAW_BASE, "claw"); part(parts, builder, AllPartialModels.ARM_CLAW_BASE_GOGGLES, "claw_goggles");
            part(parts, builder, AllPartialModels.ARM_CLAW_GRIP_UPPER, "grip_upper"); part(parts, builder, AllPartialModels.ARM_CLAW_GRIP_LOWER, "grip_lower");
        } else if (builder instanceof FlywheelBuilder) {
            part(parts, builder, AllPartialModels.FLYWHEEL, "wheel"); part(parts, builder, AllPartialModels.SHAFT, "shaft");
        } else if (builder instanceof GaugeBuilder gauge) {
            part(parts, builder, gauge.stress ? AllPartialModels.GAUGE_HEAD_STRESS : AllPartialModels.GAUGE_HEAD_SPEED, "head");
            part(parts, builder, AllPartialModels.GAUGE_DIAL, "dial"); part(parts, builder, AllPartialModels.SHAFT, "shaft");
        }
        MODELS.put(builder.id, Map.copyOf(parts));
    }
    private static void part(Map<PartialModel, PartialModel> parts, NativeKineticMachineBuilder builder, PartialModel nativeModel, String role) {
        parts.put(nativeModel, PartialModel.of(NativeMachineAssets.partModelId(builder.id, role)));
    }
    public static PartialModel select(PartialModel nativeModel, BlockState state) {
        NativeKineticMachineBuilder builder = state.getBlock() instanceof ScriptedMechanicalArmBlock arm ? arm.sourceBuilder()
                : state.getBlock() instanceof ScriptedFlywheelBlock wheel ? wheel.sourceBuilder()
                : state.getBlock() instanceof ScriptedGaugeBlock gauge ? gauge.sourceBuilder() : null;
        var models = builder == null ? null : MODELS.get(builder.id);
        return models == null ? nativeModel : models.getOrDefault(nativeModel, nativeModel);
    }
    private static SuperByteBuffer shaft(BlockState state, Direction.Axis axis) {
        return CachedBuffers.partialFacingVertical(select(AllPartialModels.SHAFT, state), state,
                Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
    }
    public static final class FlywheelRenderer extends com.simibubi.create.content.kinetics.flywheel.FlywheelRenderer {
        public FlywheelRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(FlywheelBlockEntity entity, BlockState state) { return ArmGaugeClientModels.shaft(entity.getBlockState(), getRotationAxisOf(entity)); }
    }
    public static final class GaugeRenderer extends com.simibubi.create.content.kinetics.gauge.GaugeRenderer {
        private GaugeRenderer(BlockEntityRendererProvider.Context context, GaugeBlock.Type type) { super(context, type); }
        public static GaugeRenderer speed(BlockEntityRendererProvider.Context context) { return new GaugeRenderer(context, GaugeBlock.Type.SPEED); }
        public static GaugeRenderer stress(BlockEntityRendererProvider.Context context) { return new GaugeRenderer(context, GaugeBlock.Type.STRESS); }
        @Override protected SuperByteBuffer getRotatedModel(GaugeBlockEntity entity, BlockState state) { return ArmGaugeClientModels.shaft(entity.getBlockState(), getRotationAxisOf(entity)); }
    }
    public static final class SpeedVisual extends GaugeVisual.Speed {
        public SpeedVisual(VisualizationContext context, GaugeBlockEntity entity, float tick) { super(context, entity, tick); }
        @Override protected Instancer<TransformedInstance> getHeadModel() {
            return instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(select(AllPartialModels.GAUGE_HEAD_SPEED, blockState)));
        }
    }
    public static final class StressVisual extends GaugeVisual.Stress {
        public StressVisual(VisualizationContext context, GaugeBlockEntity entity, float tick) { super(context, entity, tick); }
        @Override protected Instancer<TransformedInstance> getHeadModel() {
            return instancerProvider().instancer(InstanceTypes.TRANSFORMED, Models.partial(select(AllPartialModels.GAUGE_HEAD_STRESS, blockState)));
        }
    }
    private ArmGaugeClientModels() { }
}
