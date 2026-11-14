package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour.ProcessingResult;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;

/** Distinguishes real processing from chute/player callbacks using the same transport API. */
@Mixin(value = BeltProcessingBehaviour.class, remap = false)
public abstract class BeltActionProcessingMixin {
    @WrapMethod(method = {"handleReceivedItem", "handleHeldItem"})
    private ProcessingResult cbjs$processor(TransportedItemStack item,
            TransportedItemStackHandlerBehaviour handler, Operation<ProcessingResult> original) {
        var behaviour = (BeltProcessingBehaviour) (Object) this;
        if (!BeltEventDispatcher.needsProcessingContext() || behaviour.getWorld().isClientSide)
            return original.call(item, handler);
        var previous = BeltEventDispatcher.pushProcessor(behaviour.getWorld(), behaviour.getPos(), item, handler);
        try { return original.call(item, handler); }
        finally { BeltEventDispatcher.popProcessor(previous); }
    }
}
