package net.quiwh.cbjs.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.api.event.KineticDataModifyEvent;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import net.quiwh.cbjs.kubejs.KineticDataModifyEventJS;

/** Reloads rules atomically and enforces Wildfires-style limits on the logical server. */
public final class GearLimitManager {
    private static volatile Map<Block, KineticData> rules = Map.of();
    private static volatile long rulesVersion;
    private static final ThreadLocal<Set<KineticBlockEntity>> checking =
            ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((barrier, resources, preparationProfiler, applyProfiler, preparationExecutor, gameExecutor) ->
                CompletableFuture.completedFuture(null).thenCompose(barrier::wait)
                        .thenRunAsync(GearLimitManager::reload, gameExecutor));
    }

    public static void reload() {
        reload(event -> {
            ScriptReloadGuard.requireSuccessfulServerScripts();
            MinecraftForge.EVENT_BUS.post(event);
            CBJSEvents.KINETIC_DATA.post(new KineticDataModifyEventJS(event));
            // KubeJS logs callback exceptions; inspect the console after dispatch too.
            ScriptReloadGuard.requireSuccessfulServerScripts();
        });
    }

    static boolean reload(Consumer<KineticDataModifyEvent> listener) {
        try {
            KineticDataModifyEvent event = new KineticDataModifyEvent(GearLimitDefaults.buildRules());
            listener.accept(event);
            Map<Block, KineticData> next = event.buildRules();
            rules = next;
            rulesVersion++;
            CBJS.LOGGER.info("Loaded {} kinetic material limits", next.size());
            return true;
        } catch (RuntimeException exception) {
            CBJS.LOGGER.error("Keeping previous kinetic limits because the new rules were invalid", exception);
            return false;
        }
    }

    public static void serverStopped(ServerStoppedEvent event) {
        rules = Map.of();
        rulesVersion++;
        checking.remove();
    }

    /** Shared active rule lookup for material connections that are not themselves kinetic blocks. */
    public static KineticData rule(Block block) { return rules.get(block); }

    public static long rulesVersion() { return rulesVersion; }

    /** Only an explicit custom RPM cap or noMaxRPM disables Create's global RPM cap. */
    public static boolean usesIndependentRpmLimit(Block block) {
        KineticData data = rules.get(block);
        return data != null && data.independentRpmLimit();
    }

    public static void check(KineticBlockEntity entity) {
        KineticData data = rules.get(entity.getBlockState().getBlock());
        // Most registered kinetics have no material stress rating. A compliant RPM-only
        // rule cannot destroy anything, so it needs no world identity lookup or network read.
        if (data == null || data.maxNetworkStress() == null
                && (data.maxSpeed() == null || !data.isExceeded(entity.getSpeed(), 0))) {
            return;
        }
        if (!KineticEventDispatcher.isCurrent(entity)) {
            return;
        }
        Level level = entity.getLevel();
        double networkStress = data.maxNetworkStress() != null ? NetworkStressAccess.get(entity) : 0;
        if (!data.isExceeded(entity.getSpeed(), networkStress)) {
            return;
        }
        Set<KineticBlockEntity> active = checking.get();
        if (!active.add(entity)) return;
        try {
            if (KineticEventDispatcher.hasOverloadListeners(entity)) {
                if (KineticEventDispatcher.overloadCancelled(entity, data, networkStress)
                        || !KineticEventDispatcher.isCurrent(entity)
                        || rules.get(entity.getBlockState().getBlock()) != data) return;
                // Scripts can fix the speed/network or reload the limits. Recheck before destroying anything.
                networkStress = data.maxNetworkStress() != null ? NetworkStressAccess.get(entity) : 0;
                if (!data.isExceeded(entity.getSpeed(), networkStress)) return;
            }
            List<ItemStack> drops = data.list();
            if (level.destroyBlock(entity.getBlockPos(), drops == null) && drops != null) {
                for (ItemStack drop : drops) {
                    if (!drop.isEmpty()) {
                        var pos = entity.getBlockPos();
                        // Wildfires' explicit replacements are independent of ordinary block-loot gamerules.
                        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5, drop));
                    }
                }
            }
        } finally {
            active.remove(entity);
            // Keep the empty identity set for the next failure on this thread. Removing
            // each key releases its block entity; stop clears the small backing map.
            // Mass failures still destroy immediately without allocating a set per block.
        }
    }

    private GearLimitManager() {
    }
}
