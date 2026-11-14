package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.quiwh.cbjs.kinetics.KineticEventDispatcher;
import net.quiwh.cbjs.kinetics.CogwheelStateTracker;
import net.quiwh.cbjs.kinetics.GearLimitManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityMixin {
    @Shadow protected float stress;
    @Shadow protected float capacity;
    @Unique private CogwheelStateTracker cbjs$eventState;
    @Unique private boolean cbjs$dispatchingEvents;

    @Inject(method = {"setSpeed", "onSpeedChanged", "updateFromNetwork", "validateKinetics", "read",
            "clearKineticInformation"}, at = @At("RETURN"), remap = false)
    private void cbjs$markKineticEventsDirty(CallbackInfo ci) {
        // Until a listener needs a baseline there is no tracker to allocate or maintain.
        if (cbjs$eventState != null) cbjs$eventState.markDirty();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void cbjs$checkMaterialLimits(CallbackInfo ci) {
        KineticBlockEntity entity = (KineticBlockEntity) (Object) this;
        if (cbjs$dispatchingEvents) return;
        if (KineticEventDispatcher.hasChangeListeners(entity)) {
            if (cbjs$eventState == null || cbjs$eventState.needsObservation()) {
                if (KineticEventDispatcher.isCurrent(entity)) {
                    if (cbjs$eventState == null) cbjs$eventState = new CogwheelStateTracker();
                    var previous = cbjs$eventState.observe(entity.getSpeed(), entity.getTheoreticalSpeed(),
                            stress, capacity, entity.isOverStressed());
                    var current = cbjs$eventState.current();
                    if (previous != current) {
                        cbjs$dispatchingEvents = true;
                        try {
                            KineticEventDispatcher.changed(entity, previous, current);
                        } finally {
                            cbjs$dispatchingEvents = false;
                        }
                    }
                } else {
                    cbjs$eventState = null;
                }
            }
        } else {
            cbjs$eventState = null;
        }
        GearLimitManager.check(entity);
    }
}
