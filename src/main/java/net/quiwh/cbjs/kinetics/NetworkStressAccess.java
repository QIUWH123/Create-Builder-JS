package net.quiwh.cbjs.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.quiwh.cbjs.mixin.TorqueNetworksAccessor;

/** Implemented on Create networks by the required stress-cache mixin. */
public interface NetworkStressAccess {
    float cbjs$getStress();
    void cbjs$invalidateStress();

    static float get(KineticBlockEntity entity) {
        return entity.hasNetwork()
                ? ((NetworkStressAccess) entity.getOrCreateNetwork()).cbjs$getStress() : 0;
    }

    static void invalidate(KineticBlockEntity entity) {
        if (entity.getLevel() != null && !entity.getLevel().isClientSide && entity.hasNetwork()) {
            // Removal and world teardown may already have discarded the native network.
            // Invalidating an observation must never recreate that network or its world map.
            var networks = TorqueNetworksAccessor.cbjs$getNetworks().get(entity.getLevel());
            var network = networks == null ? null : networks.get(entity.network);
            if (network != null) ((NetworkStressAccess) network).cbjs$invalidateStress();
        }
    }
}
