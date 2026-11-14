package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.*;
import com.simibubi.create.content.kinetics.motor.*;
import com.simibubi.create.content.kinetics.waterwheel.*;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBuilder;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.createmod.catnip.render.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kubejs.*;

/** Per-registration models with native wheel wood substitution in both rendering paths. */
public final class NativeGeneratorClientModels {
    private static final Map<ResourceLocation, Map<String, PartialModel>> PARTS = new ConcurrentHashMap<>();
    public static void declare(NativeKineticMachineBuilder builder) {
        String[] roles = builder instanceof WaterWheelBuilder wheel ? wheel.large ? new String[]{"wheel", "wheel_extension"} : new String[]{"wheel"}
                : builder instanceof HandCrankBuilder ? new String[]{"base", "handle"}
                : builder instanceof WindmillBearingBuilder ? new String[]{"top", "shaft"} : new String[]{"shaft"};
        Map<String, PartialModel> parts = new java.util.HashMap<>();
        for (String role : roles) parts.put(role, PartialModel.of(NativeMachineAssets.partModelId(builder.id, role)));
        PARTS.put(builder.id, Map.copyOf(parts));
    }
    public static void register(NativeKineticMachineBuilder builder) {
        if (builder instanceof WaterWheelBuilder wheel && wheel.large)
            com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu.registerBlacklistedBlock(builder.id);
    }
    private static NativeKineticMachineBuilder builder(BlockState state) {
        if (state.getBlock() instanceof ScriptedWaterWheelBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedLargeWaterWheelBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedWindmillBearingBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedHandCrankBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedCreativeMotorBlock block) return block.sourceBuilder();
        return null;
    }
    public static PartialModel part(BlockState state, String role, PartialModel fallback) {
        var builder = builder(state);
        return builder == null ? fallback : PARTS.getOrDefault(builder.id, Map.of()).getOrDefault(role, fallback);
    }
    public static PartialModel bearingPart(PartialModel original, BlockState state) {
        return part(state, original == AllPartialModels.BEARING_TOP_WOODEN ? "top" : original == AllPartialModels.SHAFT_HALF ? "shaft" : "", original);
    }
    private static BakedModel wheelModel(BlockState state, BlockState material, boolean large) {
        String role = large && state.getValue(LargeWaterWheelBlock.EXTENSION) ? "wheel_extension" : "wheel";
        var fallback = large ? role.equals("wheel_extension") ? AllPartialModels.LARGE_WATER_WHEEL_EXTENSION : AllPartialModels.LARGE_WATER_WHEEL : AllPartialModels.WATER_WHEEL;
        return WaterWheelRenderer.generateModel(part(state, role, fallback).get(), material);
    }
    private record WheelKey(BlockState state, BlockState material, boolean large) { }
    private static final SuperByteBufferCache.Compartment<WheelKey> WHEEL_BUFFERS = new SuperByteBufferCache.Compartment<>();
    static { SuperByteBufferCache.getInstance().registerCompartment(WHEEL_BUFFERS); }
    private static final RendererReloadCache<WheelKey, Model> WHEEL_MODELS = new RendererReloadCache<>(key ->
            new BakedModelBuilder(wheelModel(key.state(), key.material(), key.large())).build());
    // Keep renderer-producing lambda bodies in a client class. Their erased return
    // type links BlockEntityRenderer when the JVM verifies the enclosing class.
    public static WheelRenderer<WaterWheelBlockEntity> smallWheelRenderer(BlockEntityRendererProvider.Context context) {
        return new WheelRenderer<>(context, false);
    }
    public static WheelRenderer<LargeWaterWheelBlockEntity> largeWheelRenderer(BlockEntityRendererProvider.Context context) {
        return new WheelRenderer<>(context, true);
    }
    public static final class WheelRenderer<T extends WaterWheelBlockEntity> extends WaterWheelRenderer<T> {
        public WheelRenderer(BlockEntityRendererProvider.Context context, boolean large) { super(context, large); }
        @Override protected SuperByteBuffer getRotatedModel(T entity, BlockState state) {
            var key = new WheelKey(state, entity.material, large);
            return SuperByteBufferCache.getInstance().get(WHEEL_BUFFERS, key, () -> {
                Direction direction = large ? Direction.get(Direction.AxisDirection.POSITIVE, state.getValue(LargeWaterWheelBlock.AXIS)) : state.getValue(WaterWheelBlock.FACING);
                return SuperBufferFactory.getInstance().createForBlock(wheelModel(state, entity.material, large), Blocks.AIR.defaultBlockState(), CachedBuffers.rotateToFaceVertical(direction).get());
            });
        }
    }
    public static final class WheelVisual<T extends WaterWheelBlockEntity> extends KineticBlockEntityVisual<T> {
        private final boolean large;
        private BlockState lastMaterial;
        private RotatingInstance wheel;
        public WheelVisual(VisualizationContext context, T entity, boolean large, float partialTick) {
            super(context, entity, partialTick); this.large = large; setup();
        }
        private void setup() {
            lastMaterial = blockEntity.material;
            wheel = instancerProvider().instancer(AllInstanceTypes.ROTATING, WHEEL_MODELS.get(new WheelKey(blockState, lastMaterial, large))).createInstance();
            wheel.setup(blockEntity).setPosition(getVisualPosition()).rotateToFace(rotationAxis()).setChanged();
        }
        @Override public void update(float partialTick) {
            if (lastMaterial != blockEntity.material) { wheel.delete(); setup(); }
            else wheel.setup(blockEntity).setChanged();
        }
        @Override public void updateLight(float partialTick) { relight(wheel); }
        @Override protected void _delete() { wheel.delete(); }
        @Override public void collectCrumblingInstances(Consumer<Instance> consumer) { consumer.accept(wheel); }
    }
    public static final class CrankRenderer extends com.simibubi.create.content.kinetics.crank.HandCrankRenderer {
        public CrankRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity entity, BlockState state) {
            return CachedBuffers.partialFacing(part(state, "base", AllPartialModels.HAND_CRANK_BASE), state);
        }
    }
    public static final class MotorRenderer extends CreativeMotorRenderer {
        public MotorRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(CreativeMotorBlockEntity entity, BlockState state) {
            return CachedBuffers.partialFacing(part(state, "shaft", AllPartialModels.SHAFT_HALF), state);
        }
    }
    public static final class MotorVisual extends OrientedRotatingVisual<CreativeMotorBlockEntity> {
        public MotorVisual(VisualizationContext context, CreativeMotorBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Direction.SOUTH, entity.getBlockState().getValue(CreativeMotorBlock.FACING), Models.partial(part(entity.getBlockState(), "shaft", AllPartialModels.SHAFT_HALF)));
        }
    }
    private NativeGeneratorClientModels() { }
}
