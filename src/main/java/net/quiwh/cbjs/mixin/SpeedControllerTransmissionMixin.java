package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedSpeedControllerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends Create's concrete-block identity check to our native controller subclasses. */
@Mixin(value = RotationPropagator.class, remap = false)
public abstract class SpeedControllerTransmissionMixin {
    @Inject(method = "isLargeCogToSpeedController", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void cbjs$controller(BlockState from, BlockState to, BlockPos diff, CallbackInfoReturnable<Boolean> callback) {
        if (!(to.getBlock() instanceof ScriptedSpeedControllerBlock)) return;
        callback.setReturnValue(ICogWheel.isLargeCog(from) && diff.equals(BlockPos.ZERO.below())
                && from.getValue(CogWheelBlock.AXIS).isHorizontal()
                && to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) != from.getValue(CogWheelBlock.AXIS));
    }
}
