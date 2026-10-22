package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticStressMixin {
    @Unique private float cbjs$stressSpeed;

    @Inject(method = {"setSpeed", "onSpeedChanged", "validateKinetics", "read"},
            at = @At("RETURN"), remap = false)
    private void cbjs$invalidateChangedSpeed(CallbackInfo ci) {
        KineticBlockEntity entity = (KineticBlockEntity) (Object) this;
        float magnitude = Math.abs(entity.getTheoreticalSpeed());
        if (magnitude != cbjs$stressSpeed) {
            cbjs$stressSpeed = magnitude;
            NetworkStressAccess.invalidate(entity);
        }
    }

    @Inject(method = "clearKineticInformation", at = @At("HEAD"), remap = false)
    private void cbjs$invalidatePreviousNetwork(CallbackInfo ci) {
        // This method clears the network reference, so invalidate before that reference is lost.
        NetworkStressAccess.invalidate((KineticBlockEntity) (Object) this);
        cbjs$stressSpeed = 0;
    }
}
