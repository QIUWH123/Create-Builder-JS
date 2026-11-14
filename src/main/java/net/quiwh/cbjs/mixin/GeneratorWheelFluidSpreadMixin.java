package net.quiwh.cbjs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.*;
import net.quiwh.cbjs.content.ScriptedWaterWheelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Apply Create's small-wheel edge-flow exclusion to scripted small wheels. */
@Mixin(value = FlowingFluid.class, remap = false)
public abstract class GeneratorWheelFluidSpreadMixin {
    @Inject(method = {"canPassThrough", "m_75963_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$edgeFlow(BlockGetter level, Fluid fluid, BlockPos from, BlockState fromState, Direction direction,
            BlockPos to, BlockState toState, FluidState fluidState, CallbackInfoReturnable<Boolean> ci) {
        if (direction.getAxis() == Direction.Axis.Y) return;
        var below = level.getBlockState(from.below());
        if (below.getBlock() instanceof ScriptedWaterWheelBlock wheel && wheel.getRotationAxis(below) == direction.getAxis())
            ci.setReturnValue(false);
    }
}
