package net.quiwh.cbjs.mixin;

import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.kinetics.KineticStressValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Override the native base value so machine-specific calculations retain their normal factors. */
@Mixin(value = BlockStressValues.class, remap = false)
public abstract class KineticStressValuesMixin {
    @Inject(method = "getImpact", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$impact(Block block, CallbackInfoReturnable<Double> ci) {
        var value = KineticStressValues.getOverride(block, false);
        if (value != null) ci.setReturnValue(value);
    }

    @Inject(method = "getCapacity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$capacity(Block block, CallbackInfoReturnable<Double> ci) {
        var value = KineticStressValues.getOverride(block, true);
        if (value != null) ci.setReturnValue(value);
    }
}
