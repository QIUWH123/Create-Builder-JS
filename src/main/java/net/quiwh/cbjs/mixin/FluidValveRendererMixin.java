package net.quiwh.cbjs.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.quiwh.cbjs.client.FluidDeviceClientModels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reuses the native valve animation with the scripted pointer's texture. */
@Mixin(value = FluidValveRenderer.class, remap = false)
public abstract class FluidValveRendererMixin {
    @Redirect(method = "renderSafe(Lcom/simibubi/create/content/fluids/pipes/valve/FluidValveBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/AllPartialModels;FLUID_VALVE_POINTER:Ldev/engine_room/flywheel/lib/model/baked/PartialModel;"),
            remap = false, require = 1, allow = 1)
    private PartialModel cbjs$materialPointer(FluidValveBlockEntity entity, float partialTick,
            PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        return FluidDeviceClientModels.valvePointer(entity.getBlockState(), AllPartialModels.FLUID_VALVE_POINTER);
    }
}
