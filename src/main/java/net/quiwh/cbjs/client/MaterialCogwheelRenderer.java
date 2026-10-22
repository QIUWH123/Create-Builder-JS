package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MaterialGear;

/** Uses the same material partials as the Flywheel visual. */
public final class MaterialCogwheelRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {
    public MaterialCogwheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(T entity, BlockState state) {
        MaterialGear gear = (MaterialGear) state.getBlock();
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        return CachedBuffers.partialFacingVertical(models.rotating(gear.isLargeCog(), gear.isEncased()), state,
                Direction.fromAxisAndDirection(getRotationAxisOf(entity), Direction.AxisDirection.POSITIVE));
    }

    @Override
    protected void renderSafe(T entity, float partialTicks, PoseStack poseStack, MultiBufferSource buffers,
                              int light, int overlay) {
        super.renderSafe(entity, partialTicks, poseStack, buffers, light, overlay);
        if (VisualizationManager.supportsVisualization(entity.getLevel())) {
            return;
        }
        BlockState state = entity.getBlockState();
        MaterialGear gear = (MaterialGear) state.getBlock();
        if (gear.isShaftless() || gear.models().usesCompleteModel(gear.isEncased())) {
            return;
        }
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        Direction.Axis axis = getRotationAxisOf(entity);
        float angle = gear.isLargeCog()
                ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft((SimpleKineticBlockEntity) entity, axis)
                : getAngleForBe(entity, entity.getBlockPos(), axis);
        if (!gear.isEncased()) {
            SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(models.shaft(), state,
                    Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
            kineticRotationTransform(shaft, entity, axis, angle, light)
                    .renderInto(poseStack, buffers.getBuffer(RenderType.solid()));
            return;
        }
        IRotate rotating = (IRotate) state.getBlock();
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == axis && rotating.hasShaftTowards(entity.getLevel(), entity.getBlockPos(),
                    state, direction)) {
                SuperByteBuffer shaft = CachedBuffers.partialFacing(models.shaftHalf(), state, direction);
                kineticRotationTransform(shaft, entity, axis, angle, light)
                        .renderInto(poseStack, buffers.getBuffer(RenderType.solid()));
            }
        }
    }
}
