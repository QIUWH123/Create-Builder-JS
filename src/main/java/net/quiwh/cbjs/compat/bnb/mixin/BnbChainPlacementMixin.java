package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelChain;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.compat.bnb.BnbChainedCogwheelBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlacingCogwheelChain.class, remap = false)
public abstract class BnbChainPlacementMixin {
    @Inject(method = "isValidBlockTarget", at = @At("HEAD"), cancellable = true)
    private static void cbjs$acceptSource(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if (BnbChainedCogwheelBuilder.forSource(state) != null) callback.setReturnValue(true);
    }

    @Inject(method = "isLargeBlockTarget", at = @At("HEAD"), cancellable = true)
    private static void cbjs$sourceSize(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        var block = BnbChainedCogwheelBuilder.forSource(state);
        if (block != null) callback.setReturnValue(block.isLargeCog());
    }

    @Inject(method = "hasSmallCogwheelOffset", at = @At("HEAD"), cancellable = true)
    private static void cbjs$sourceOffset(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        var block = BnbChainedCogwheelBuilder.forSource(state);
        if (block != null) callback.setReturnValue(!block.isLargeCog());
    }
}
