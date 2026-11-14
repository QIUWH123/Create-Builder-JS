package net.quiwh.cbjs.kinetics;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.network.KineticStressPacket;

/** Logical-side separation also keeps an integrated server's rules out of client lookups. */
public final class KineticStressValues {
    private static volatile Map<Block, KineticStressPacket.Values> clientValues = Map.of();

    public static Double getOverride(Block block, boolean capacity) {
        boolean client = FMLEnvironment.dist == Dist.CLIENT && EffectiveSide.get().isClient();
        return getOverride(block, capacity, client);
    }

    static Double getOverride(Block block, boolean capacity, boolean client) {
        if (client) {
            var values = clientValues.get(block);
            return values == null ? null : capacity ? values.capacity() : values.impact();
        }
        var rule = GearLimitManager.rule(block);
        return rule == null ? null : capacity ? rule.stressCapacity() : rule.stressImpact();
    }

    /** Invoked on the client main thread only; unknown registry IDs cannot alias the default block. */
    public static void applyClient(KineticStressPacket packet) {
        var next = new HashMap<Block, KineticStressPacket.Values>();
        packet.values().forEach((id, values) -> {
            if (BuiltInRegistries.BLOCK.containsKey(id)) next.put(BuiltInRegistries.BLOCK.get(id), values);
        });
        clientValues = Map.copyOf(next);
    }

    public static void clearClient() { clientValues = Map.of(); }

    private KineticStressValues() { }
}
