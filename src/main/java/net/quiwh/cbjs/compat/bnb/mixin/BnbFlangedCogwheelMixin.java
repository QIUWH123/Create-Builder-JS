package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.block.IFlangedCogWheel;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.MaterialGear;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reuse BnB's native flanged meshing rules without linking common blocks to its interface. */
@Mixin(value = IFlangedCogWheel.class, remap = false)
public interface BnbFlangedCogwheelMixin {
    @Inject(method = "isFlanged(Lnet/minecraft/world/level/block/Block;)Z", at = @At("HEAD"), cancellable = true)
    private static void cbjs$recognizeFlanged(Block block, CallbackInfoReturnable<Boolean> callback) {
        if (block instanceof MaterialGear gear && gear.isFlanged()) callback.setReturnValue(true);
    }
}
