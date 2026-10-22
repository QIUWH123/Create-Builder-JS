package net.quiwh.cbjs.compat.bnb.client;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.client.ModPartialModels;
import net.quiwh.cbjs.content.MaterialGear;

/** Reuses BnB's controller chain rendering while both gear paths use the source material. */
public final class BnbChainedCogwheelRenderer extends CogwheelChainBlockEntityRenderer {
    public BnbChainedCogwheelRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected SuperByteBuffer getRotatedModel(CogwheelChainBlockEntity entity, BlockState state) {
        MaterialGear gear = (MaterialGear) state.getBlock();
        ModPartialModels.GearModels models = ModPartialModels.of(gear.models());
        return CachedBuffers.partialFacingVertical(models.rotating(gear.isLargeCog(), gear.isEncased()), state,
                Direction.fromAxisAndDirection(getRotationAxisOf(entity), Direction.AxisDirection.POSITIVE));
    }

    @Override
    protected void renderSafe(CogwheelChainBlockEntity entity, float partialTicks, PoseStack poseStack,
                              MultiBufferSource buffers, int light, int overlay) {
        // BnB draws the chain after Create's gear pass, including when Flywheel owns the gear.
        super.renderSafe(entity, partialTicks, poseStack, buffers, light, overlay);
        if (VisualizationManager.supportsVisualization(entity.getLevel())) return;
        BlockState state = entity.getBlockState();
        MaterialGear gear = (MaterialGear) state.getBlock();
        if (gear.isShaftless() || gear.models().usesCompleteModel(gear.isEncased())) return;
        Direction.Axis axis = getRotationAxisOf(entity);
        float angle = gear.isLargeCog()
                ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft(entity, axis)
                : getAngleForBe(entity, entity.getBlockPos(), axis);
        SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(ModPartialModels.of(gear.models()).shaft(), state,
                Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        kineticRotationTransform(shaft, entity, axis, angle, light)
                .renderInto(poseStack, buffers.getBuffer(RenderType.solid()));
    }
}
