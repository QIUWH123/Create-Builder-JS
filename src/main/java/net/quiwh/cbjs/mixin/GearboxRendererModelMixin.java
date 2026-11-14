package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.client.GearboxClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Substitutes the material partial while retaining the native gearbox animation. */
@Mixin(value = GearboxRenderer.class, remap = false)
public abstract class GearboxRendererModelMixin {
    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/kinetics/gearbox/GearboxBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/AllPartialModels;SHAFT_HALF:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$shaftModel(GearboxBlockEntity entity, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return GearboxClientModels.shaft(entity.getBlockState(), AllPartialModels.SHAFT_HALF);
    }
}
