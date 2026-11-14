package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;
import net.quiwh.cbjs.kinetics.SavedKineticCapacity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticStressMixin implements SavedKineticCapacity {
    @Shadow protected float lastCapacityProvided;
    @Unique private float cbjs$stressSpeed;
    @Unique private float cbjs$savedCapacity = Float.NaN;
    @Unique private boolean cbjs$pendingCapacity;

    @Override
    public float cbjs$consumeSavedCapacity(float lastCapacity) {
        if (!cbjs$pendingCapacity) return Float.NaN;
        cbjs$pendingCapacity = false;
        float saved = cbjs$savedCapacity;
        cbjs$savedCapacity = Float.NaN;
        // Legacy coefficients can remain positive after a generator has stopped. Its old
        // actual SU cannot be inferred safely; leave native subtraction intact for old saves.
        return saved;
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false)
    private void cbjs$saveCapacity(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        if (clientPacket || !compound.contains("Network", Tag.TAG_COMPOUND)) return;
        KineticBlockEntity entity = (KineticBlockEntity) (Object) this;
        float contribution = cbjs$pendingCapacity ? cbjs$savedCapacity : Float.NaN;
        if (cbjs$pendingCapacity && Float.isNaN(contribution)) return;
        if (Float.isNaN(contribution)) contribution = lastCapacityProvided * Math.abs(entity.getGeneratedSpeed());
        if (Float.isFinite(contribution) && contribution >= 0)
            compound.getCompound("Network").putFloat("CBJSSourceCapacity", contribution);
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void cbjs$readCapacity(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        KineticBlockEntity entity = (KineticBlockEntity) (Object) this;
        if (clientPacket || !entity.hasNetwork()) return;
        cbjs$pendingCapacity = true;
        CompoundTag network = compound.getCompound("Network");
        if (!network.contains("CBJSSourceCapacity", Tag.TAG_ANY_NUMERIC)) return;
        float contribution = network.getFloat("CBJSSourceCapacity");
        if (Float.isFinite(contribution) && contribution >= 0) cbjs$savedCapacity = contribution;
    }

    @Inject(method = "setNetwork", at = @At("HEAD"), remap = false)
    private void cbjs$discardPreviousContribution(Long network, CallbackInfo ci) {
        if (!java.util.Objects.equals(((KineticBlockEntity) (Object) this).network, network)) {
            cbjs$savedCapacity = Float.NaN;
            cbjs$pendingCapacity = false;
        }
    }

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
        cbjs$savedCapacity = Float.NaN;
        cbjs$pendingCapacity = false;
    }
}
