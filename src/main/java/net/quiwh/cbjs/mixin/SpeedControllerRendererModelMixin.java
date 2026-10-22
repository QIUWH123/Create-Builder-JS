package net.quiwh.cbjs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.quiwh.cbjs.client.TransmissionClientModels;
import net.quiwh.cbjs.content.ScriptedSpeedControllerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpeedControllerRenderer.class, remap = false)
public abstract class SpeedControllerRendererModelMixin {
    @Inject(method = "getRotatedModel", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void cbjs$shaft(SpeedControllerBlockEntity entity, CallbackInfoReturnable<SuperByteBuffer> callback) {
        if (!(entity.getBlockState().getBlock() instanceof ScriptedSpeedControllerBlock)) return;
        var facing = net.minecraft.core.Direction.fromAxisAndDirection(KineticBlockEntityRenderer.getRotationAxisOf(entity), net.minecraft.core.Direction.AxisDirection.POSITIVE);
        callback.setReturnValue(CachedBuffers.partialDirectional(TransmissionClientModels.shaft(entity.getBlockState(), AllPartialModels.SHAFT),
                entity.getBlockState(), facing, () -> {
                    PoseStack pose = new PoseStack();
                    pose.translate(0.5, 0.5, 0.5);
                    pose.mulPose(net.quiwh.cbjs.client.ChainDriveClientModels.shaftRotation(facing.getAxis()));
                    pose.translate(-0.5, -0.5, -0.5);
                    return pose;
                }));
    }
    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/speedController/SpeedControllerBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;SPEED_CONTROLLER_BRACKET:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private PartialModel cbjs$bracket(SpeedControllerBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return TransmissionClientModels.bracket(entity.getBlockState(), AllPartialModels.SPEED_CONTROLLER_BRACKET);
    }
}
