package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedChainDriveBlock;
import net.quiwh.cbjs.kubejs.ChainDriveBuilder;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Both render modes share a material shaft while retaining native kinetic animation. */
public final class ChainDriveClientModels {
    private static final Map<ResourceLocation, PartialModel> SHAFTS = new ConcurrentHashMap<>();

    public static ResourceLocation shaftLocation(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/shaft");
    }
    public static void register(ChainDriveBuilder builder) {
        SHAFTS.computeIfAbsent(builder.id, id -> PartialModel.of(shaftLocation(id)));
    }
    public static PartialModel shaft(BlockState state) {
        if (!(state.getBlock() instanceof ScriptedChainDriveBlock drive)) return AllPartialModels.SHAFT;
        return SHAFTS.getOrDefault(drive.sourceBuilder().id, AllPartialModels.SHAFT);
    }

    /** Match SingleAxisRotatingVisual's UP-to-axis rotation for asymmetric models too. */
    public static Quaternionf shaftRotation(Direction.Axis axis) {
        Direction direction = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        return new Quaternionf().rotationTo(0, 1, 0,
                direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    public static final class ChainDriveRenderer extends KineticBlockEntityRenderer<KineticBlockEntity> {
        public ChainDriveRenderer(BlockEntityRendererProvider.Context context) { super(context); }
        @Override protected SuperByteBuffer getRotatedModel(KineticBlockEntity entity, BlockState state) {
            Direction direction = Direction.fromAxisAndDirection(getRotationAxisOf(entity), Direction.AxisDirection.POSITIVE);
            return CachedBuffers.partialDirectional(ChainDriveClientModels.shaft(state), state, direction, () -> {
                PoseStack pose = new PoseStack();
                pose.translate(0.5, 0.5, 0.5);
                pose.mulPose(shaftRotation(direction.getAxis()));
                pose.translate(-0.5, -0.5, -0.5);
                return pose;
            });
        }
    }
    public static final class ChainDriveVisual extends SingleAxisRotatingVisual<KineticBlockEntity> {
        public ChainDriveVisual(VisualizationContext context, KineticBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Models.partial(ChainDriveClientModels.shaft(entity.getBlockState())));
        }
    }
    private ChainDriveClientModels() { }
}
