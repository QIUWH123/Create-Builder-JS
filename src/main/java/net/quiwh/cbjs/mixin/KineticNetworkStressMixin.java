package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.Map;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;
import net.quiwh.cbjs.kinetics.NetworkStressSnapshot;
import net.quiwh.cbjs.kinetics.SavedKineticCapacity;
import net.quiwh.cbjs.kinetics.StressTrackedMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkStressMixin implements NetworkStressAccess {
    @Shadow public Map<KineticBlockEntity, Float> members;
    @Shadow private float unloadedCapacity;
    @Shadow public abstract float calculateStress();
    @Unique private NetworkStressSnapshot cbjs$stressSnapshot;
    @Unique private Map<KineticBlockEntity, Float> cbjs$trackedMembers;

    @Inject(method = "addSilently", at = @At("HEAD"), remap = false)
    private void cbjs$subtractSavedCapacity(KineticBlockEntity entity, float lastCapacity,
                                          float lastStress, CallbackInfo ci) {
        if (members.containsKey(entity)) return;
        float saved = ((SavedKineticCapacity) entity).cbjs$consumeSavedCapacity(lastCapacity);
        if (!Float.isFinite(saved) || saved < 0) return;
        // Native subtracts only current sources at their current generated RPM. Compensate
        // that subtraction so this member removes exactly its own saved SU, once.
        float nativeContribution = entity.isSource()
                ? lastCapacity * Math.abs(entity.getGeneratedSpeed()) : 0;
        unloadedCapacity += nativeContribution - saved;
    }

    @Override
    public float cbjs$getStress() {
        if (cbjs$stressSnapshot == null) {
            cbjs$stressSnapshot = new NetworkStressSnapshot(this::calculateStress);
        }
        if (members != cbjs$trackedMembers) {
            // Track only networks whose demand is requested; ordinary networks retain their map.
            members = new StressTrackedMap<>(members, cbjs$stressSnapshot::invalidate);
            cbjs$trackedMembers = members;
            cbjs$stressSnapshot.invalidate();
        }
        return cbjs$stressSnapshot.get();
    }

    @Override
    public void cbjs$invalidateStress() {
        if (cbjs$stressSnapshot != null) cbjs$stressSnapshot.invalidate();
    }

    @Inject(method = "calculateStress", at = @At("RETURN"), remap = false)
    private void cbjs$rememberNativeDemand(CallbackInfoReturnable<Float> ci) {
        if (cbjs$stressSnapshot != null) cbjs$stressSnapshot.record(ci.getReturnValueF());
    }
}
