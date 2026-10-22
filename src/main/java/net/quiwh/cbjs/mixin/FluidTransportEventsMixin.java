package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import net.quiwh.cbjs.fluids.FluidPipeEventDispatcher;
import net.quiwh.cbjs.fluids.PipeStateTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidTransportBehaviour.class, remap = false)
public abstract class FluidTransportEventsMixin {
    @Unique private PipeStateTracker cbjs$pipeEvents;
    @Unique private boolean cbjs$dispatchingPipeEvents;

    @Inject(method = "tick", at = @At("RETURN"), remap = false)
    private void cbjs$observePipeEvents(CallbackInfo ci) {
        if (cbjs$dispatchingPipeEvents) return;
        cbjs$dispatchingPipeEvents = true;
        try {
            cbjs$pipeEvents = FluidPipeEventDispatcher.observe(
                    (FluidTransportBehaviour) (Object) this, cbjs$pipeEvents);
        } finally {
            cbjs$dispatchingPipeEvents = false;
        }
    }
}
