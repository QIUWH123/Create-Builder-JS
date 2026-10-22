package net.quiwh.cbjs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.quiwh.cbjs.client.TransmissionClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = SplitShaftRenderer.class, remap = false)
public abstract class TransmissionRendererModelMixin {
    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/transmission/SplitShaftBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;SHAFT_HALF:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"), require = 1, allow = 1)
    private PartialModel cbjs$material(SplitShaftBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return TransmissionClientModels.shaft(entity.getBlockState(), AllPartialModels.SHAFT_HALF);
    }
}
