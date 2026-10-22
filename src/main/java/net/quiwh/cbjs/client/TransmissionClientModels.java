package net.quiwh.cbjs.client;

import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kubejs.TransmissionBuilder;

/** Native animators use the same generated partials in buffered and instanced rendering. */
public final class TransmissionClientModels {
    private static final Map<ResourceLocation, PartialModel> SHAFTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, PartialModel> BRACKETS = new ConcurrentHashMap<>();
    public static void register(TransmissionBuilder builder) {
        boolean controller = builder.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER;
        SHAFTS.computeIfAbsent(builder.id, id -> PartialModel.of(location(id, controller ? "shaft" : "shaft_half")));
        if (controller) BRACKETS.computeIfAbsent(builder.id, id -> PartialModel.of(location(id, "bracket")));
    }
    private static ResourceLocation location(ResourceLocation id, String role) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/" + role);
    }
    private static TransmissionBuilder builder(BlockState state) {
        if (state.getBlock() instanceof ScriptedClutchBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedGearshiftBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedSpeedControllerBlock block) return block.sourceBuilder();
        return null;
    }
    public static PartialModel shaft(BlockState state, PartialModel fallback) {
        TransmissionBuilder builder = builder(state);
        return builder == null ? fallback : SHAFTS.getOrDefault(builder.id, fallback);
    }
    public static PartialModel bracket(BlockState state, PartialModel fallback) {
        TransmissionBuilder builder = builder(state);
        return builder == null ? fallback : BRACKETS.getOrDefault(builder.id, fallback);
    }
    public static final class ControllerVisual extends SingleAxisRotatingVisual<SpeedControllerBlockEntity> {
        public ControllerVisual(VisualizationContext context, SpeedControllerBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Models.partial(TransmissionClientModels.shaft(entity.getBlockState(), com.simibubi.create.AllPartialModels.SHAFT)));
        }
    }
    private TransmissionClientModels() { }
}
