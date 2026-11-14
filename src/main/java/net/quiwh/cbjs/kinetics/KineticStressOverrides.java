package net.quiwh.cbjs.kinetics;

import com.simibubi.create.content.kinetics.KineticNetwork;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.mixin.TorqueNetworksAccessor;

/** Reload-time refresh of native coefficients; never installs or replaces native suppliers. */
public final class KineticStressOverrides {
    private static volatile long capacityVersion;

    public static long capacityVersion() { return capacityVersion; }

    static void capacityRulesChanged(Map<Block, KineticData> previous, Map<Block, KineticData> next) {
        for (Block block : previous.keySet()) {
            if (!Objects.equals(previous.get(block).stressCapacity(),
                    next.containsKey(block) ? next.get(block).stressCapacity() : null)) {
                capacityVersion++;
                return;
            }
        }
        for (Block block : next.keySet()) {
            if (!previous.containsKey(block) && next.get(block).stressCapacity() != null) {
                capacityVersion++;
                return;
            }
        }
    }

    static boolean changed(Map<Block, KineticData> previous, Map<Block, KineticData> next) {
        for (var entry : previous.entrySet()) {
            if (different(entry.getValue(), next.get(entry.getKey()))) return true;
        }
        for (var entry : next.entrySet()) {
            if (!previous.containsKey(entry.getKey()) && different(null, entry.getValue())) return true;
        }
        return false;
    }

    private static boolean different(KineticData left, KineticData right) {
        return !Objects.equals(left == null ? null : left.stressImpact(), right == null ? null : right.stressImpact())
                || !Objects.equals(left == null ? null : left.stressCapacity(), right == null ? null : right.stressCapacity());
    }

    static void refreshLoadedNetworks() {
        // One scan on a successful stress-setting reload, including removal. Native generators
        // can use another block's capacity key (steam engines), so visit every loaded network.
        for (var world : List.copyOf(TorqueNetworksAccessor.cbjs$getNetworks().entrySet())) {
            if (world.getKey().isClientSide()) continue;
            for (KineticNetwork network : List.copyOf(world.getValue().values())) refresh(network);
        }
    }

    static void refresh(KineticNetwork network) {
        // Publish both coefficient maps before syncing, avoiding an intermediate stall caused by
        // a half-updated network. Native calculation retains unloaded contributions and cleans
        // up stale entities; loading a chunk recalculates its own coefficients as usual.
        network.members.replaceAll((entity, old) -> entity.calculateStressApplied());
        network.sources.replaceAll((entity, old) -> entity.calculateAddedStressCapacity());
        if (network instanceof NetworkStressAccess access) access.cbjs$invalidateStress();
        network.updateNetwork();
        // Native updateFromNetwork sends only when overstress toggles. A value-only reload
        // still needs to refresh the client totals used by goggles and generator readouts.
        for (var entity : List.copyOf(network.members.keySet())) {
            if (KineticEventDispatcher.isCurrent(entity)) entity.sendData();
        }
    }

    private KineticStressOverrides() { }
}
