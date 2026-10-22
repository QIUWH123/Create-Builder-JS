package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.FluidReactions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.quiwh.cbjs.fluids.FluidPipeCollisionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidReactions.class, remap = false)
public abstract class FluidReactionsMixin {
    @Inject(method = "handlePipeFlowCollision", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$beforePipeCollision(Level level, BlockPos pos, FluidStack fluid,
                                                       FluidStack otherFluid, CallbackInfo callback) {
        if (FluidPipeCollisionEvents.collisionCancelled(level, pos, fluid, otherFluid)) callback.cancel();
    }
}
