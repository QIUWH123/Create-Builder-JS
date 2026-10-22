package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import com.simibubi.create.foundation.render.RenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.quiwh.cbjs.compat.createchain.ConveyorChains;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses native chain geometry, scrolling, distant rendering and lighting for each material. */
@Mixin(value = ChainConveyorRenderer.class, remap = false)
public abstract class CustomChainConveyorRendererMixin {
    @Redirect(method = "renderChains", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorRenderer;renderChain(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;FFIIZ)V"),
            remap = false, require = 1, allow = 1)
    private void cbjs$drawMaterial(PoseStack pose, MultiBufferSource buffers, float animation, float length,
            int light1, int light2, boolean far, @Local(argsOnly = true) ChainConveyorBlockEntity entity,
            @Local(ordinal = 0) BlockPos target) {
        var material = ConveyorChains.material(entity, target);
        if (!ChainMaterials.VANILLA.equals(material)) {
            var renderType = RenderTypes.chain(ChainMaterials.texture(material));
            MultiBufferSource selected = ignored -> buffers.getBuffer(renderType);
            ChainConveyorRenderer.renderChain(pose, selected, animation, length, light1, light2, far);
        } else ChainConveyorRenderer.renderChain(pose, buffers, animation, length, light1, light2, far);
    }
}
