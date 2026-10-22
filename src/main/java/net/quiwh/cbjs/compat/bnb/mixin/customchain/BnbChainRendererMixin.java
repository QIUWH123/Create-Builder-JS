package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntityRenderer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainMaterialContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CogwheelChainBlockEntityRenderer.class, remap = false)
public abstract class BnbChainRendererMixin {
    @Shadow @Final public static ResourceLocation CHAIN_LOCATION;
    @WrapMethod(method = "renderSafe(Lcom/kipti/bnb/content/cogwheel_chain/block/CogwheelChainBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V")
    private void cbjs$renderMaterial(CogwheelChainBlockEntity entity, float partialTicks, PoseStack poses,
                                          MultiBufferSource buffer, int light, int overlay, Operation<Void> original) {
        if (!entity.isController() || entity.getChain() == null) {
            original.call(entity, partialTicks, poses, buffer, light, overlay);
            return;
        }
        try (var ignored = BnbChainMaterialContext.rendering(BnbChainMaterialContext.material(entity))) {
            original.call(entity, partialTicks, poses, buffer, light, overlay);
        }
    }
    @Redirect(method = {"renderChainSlowerButWithoutGaps", "renderChainFastButWithGaps"},
            at = @At(value = "FIELD", target = "Lcom/kipti/bnb/content/cogwheel_chain/block/CogwheelChainBlockEntityRenderer;CHAIN_LOCATION:Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation cbjs$chainTexture() {
        ResourceLocation texture = BnbChainMaterialContext.renderingTexture();
        return texture == null ? CHAIN_LOCATION : texture;
    }
}
