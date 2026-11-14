package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.quiwh.cbjs.content.DirectionalGearboxBlockEntity;
import net.quiwh.cbjs.content.ScriptedDirectionalGearboxBlock;
import net.quiwh.cbjs.kinetics.GearboxTransmission;

/** Renders each exposed half-shaft at its actual transmission speed. */
public final class DirectionalGearboxRenderer extends KineticBlockEntityRenderer<DirectionalGearboxBlockEntity> {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final float DEGREES_TO_RADIANS = (float) Math.PI / 180;

    public DirectionalGearboxRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(DirectionalGearboxBlockEntity entity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffers, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(entity.getLevel())) return;
        BlockState state = entity.getBlockState();
        var kind = ((ScriptedDirectionalGearboxBlock) state.getBlock()).sourceBuilder().kind;
        Direction.Axis boxAxis = state.getValue(BlockStateProperties.AXIS);
        var model = GearboxClientModels.shaft(state, AllPartialModels.SHAFT_HALF);
        var vertices = buffers.getBuffer(RenderType.solid());
        float time = AnimationTickHolder.getRenderTime(entity.getLevel());
        for (Direction direction : DIRECTIONS) {
            if (!GearboxTransmission.hasPort(kind, boxAxis, direction)) continue;
            Direction.Axis shaftAxis = direction.getAxis();
            float speed = entity.getOutputSpeed(direction);
            float offset = getRotationOffsetForPosition(entity, entity.getBlockPos(), shaftAxis);
            float angle = ((time * speed * 0.3f + offset) % 360) * DEGREES_TO_RADIANS;
            SuperByteBuffer shaft = CachedBuffers.partialFacing(model, state, direction);
            kineticRotationTransform(shaft, entity, shaftAxis, angle, light)
                    .renderInto(poseStack, vertices);
        }
    }
}
