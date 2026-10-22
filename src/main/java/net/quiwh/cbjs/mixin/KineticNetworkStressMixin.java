package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.Map;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;
import net.quiwh.cbjs.kinetics.NetworkStressSnapshot;
import net.quiwh.cbjs.kinetics.StressTrackedMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkStressMixin implements NetworkStressAccess {
    @Shadow public Map<KineticBlockEntity, Float> members;
    @Shadow public abstract float calculateStress();
    @Unique private NetworkStressSnapshot cbjs$stressSnapshot;
    @Unique private Map<KineticBlockEntity, Float> cbjs$trackedMembers;

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
