package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.fan.AirCurrent;
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AirCurrent.class, remap = false)
public abstract class BeltActionFanMixin {
    @Shadow @Final public IAirCurrentSource source;
    @WrapMethod(method = "tickAffectedHandlers")
    private void cbjs$fanProcessor(Operation<Void> original) {
        var world = source.getAirCurrentWorld();
        if (!BeltEventDispatcher.needsProcessingContext() || world == null || world.isClientSide) {
            original.call();
            return;
        }
        var previous = BeltEventDispatcher.pushProcessor(world, source.getAirCurrentPos());
        try { original.call(); }
        finally { BeltEventDispatcher.popProcessor(previous); }
    }
}
