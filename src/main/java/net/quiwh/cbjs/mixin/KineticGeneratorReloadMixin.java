package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.quiwh.cbjs.kinetics.KineticEventDispatcher;
import net.quiwh.cbjs.kinetics.KineticStressOverrides;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Capacity-dependent generators can leave their network at zero and must restart on removal. */
@Mixin(value = GeneratingKineticBlockEntity.class, remap = false)
public abstract class KineticGeneratorReloadMixin {
    // A generator loaded after a reload may have been saved stopped under an old capacity.
    @Unique private long cbjs$capacityVersion = -1;

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void cbjs$refreshGeneration(CallbackInfo ci) {
        var generator = (GeneratingKineticBlockEntity) (Object) this;
        long version = KineticStressOverrides.capacityVersion();
        if (cbjs$capacityVersion == version) return;
        // The base kinetic tick can destroy or replace this generator before this tail runs.
        if (!KineticEventDispatcher.isCurrent(generator)) return;
        cbjs$capacityVersion = version;
        generator.updateGeneratedRotation();
    }
}
