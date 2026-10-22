package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.quiwh.cbjs.content.ScriptedSpeedControllerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Large cogs placed above scripted controllers receive the native perpendicular orientation. */
@Mixin(value = CogWheelBlock.class, remap = false)
public abstract class SpeedControllerCogPlacementMixin {
    @Inject(method = "getAxisForPlacement", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void cbjs$axis(BlockPlaceContext context, CallbackInfoReturnable<Direction.Axis> callback) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return;
        if (!((ICogWheel) this).isLargeCog()) return;
        var below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (below.getBlock() instanceof ScriptedSpeedControllerBlock)
            callback.setReturnValue(below.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
    }
}
