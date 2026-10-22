package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.CustomEncasedShaftBlock;
import net.quiwh.cbjs.content.ScriptedShaftBlock;
import net.quiwh.cbjs.kubejs.ShaftBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** One partial per material, shared by bare and encased shafts in both rendering paths. */
public final class ShaftClientModels {
    private static final Map<ResourceLocation, PartialModel> PARTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, PartialModel> BELT_PULLEYS = new ConcurrentHashMap<>();
    public static void register(ShaftBuilder builder) {
        PARTS.computeIfAbsent(builder.id, id -> PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), "block/" + id.getPath() + "/block")));
        BELT_PULLEYS.computeIfAbsent(builder.id, id -> PartialModel.of(ResourceLocation.fromNamespaceAndPath(
                id.getNamespace(), "block/" + id.getPath() + "/belt_pulley")));
        com.simibubi.create.foundation.data.CreateRegistrate.blockModel(
                () -> com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel::new).accept(builder.get());
    }
    public static PartialModel beltPulley(BlockState shaft) {
        if (!(shaft.getBlock() instanceof ScriptedShaftBlock block) || block.sourceBuilder() == null)
            return AllPartialModels.BELT_PULLEY;
        return BELT_PULLEYS.getOrDefault(block.sourceBuilder().id, AllPartialModels.BELT_PULLEY);
    }
    private static PartialModel model(BlockState state) {
        ShaftBuilder builder = state.getBlock() instanceof ScriptedShaftBlock shaft ? shaft.sourceBuilder()
                : ((CustomEncasedShaftBlock) state.getBlock()).sourceBuilder();
        return builder == null ? AllPartialModels.SHAFT : PARTS.get(builder.id);
    }
    public static final class ShaftRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
        public ShaftRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(T entity, BlockState state) {
            return CachedBuffers.partialFacingVertical(model(state), state,
                    Direction.fromAxisAndDirection(getRotationAxisOf(entity), Direction.AxisDirection.POSITIVE));
        }
    }
    public static final class ShaftVisual extends SingleAxisRotatingVisual<BracketedKineticBlockEntity> {
        public ShaftVisual(VisualizationContext context, BracketedKineticBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Models.partial(model(entity.getBlockState())));
        }
    }
    public static final class EncasedShaftVisual extends SingleAxisRotatingVisual<KineticBlockEntity> {
        public EncasedShaftVisual(VisualizationContext context, KineticBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Models.partial(model(entity.getBlockState())));
        }
    }
    private ShaftClientModels() { }
}
