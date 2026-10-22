package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Includes native PumpBlock subclasses when pipe topology changes invalidate pump pressure. */
@Mixin(value = FluidPropagator.class, remap = false)
public abstract class FluidPropagatorPumpMixin {
    @Redirect(method = "propagateChangedPipe", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            remap = false, require = 1, allow = 1)
    private static boolean cbjs$recognizeRegisteredPump(BlockEntry<?> ignored, BlockState state) {
        return PumpBlock.isPump(state);
    }
}
