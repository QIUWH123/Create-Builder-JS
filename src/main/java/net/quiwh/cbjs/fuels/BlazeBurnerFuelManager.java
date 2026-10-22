package net.quiwh.cbjs.fuels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Rule;
import net.quiwh.cbjs.kinetics.ScriptReloadGuard;
import net.quiwh.cbjs.kubejs.BlazeBurnerFuelEventJS;
import net.quiwh.cbjs.kubejs.CBJSEvents;

/** Publishes custom fuel snapshots atomically without changing native Create fuel tags. */
public final class BlazeBurnerFuelManager {
    private static final FuelSnapshot EMPTY = new FuelSnapshot(Map.of(), List.of());
    private static volatile FuelSnapshot rules = EMPTY;

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((barrier, resources, preparationProfiler, applyProfiler, preparationExecutor, gameExecutor) ->
                CompletableFuture.completedFuture(null).thenCompose(barrier::wait)
                        .thenRunAsync(BlazeBurnerFuelManager::reload, gameExecutor));
    }

    public static void reload() {
        reload(event -> {
            ScriptReloadGuard.requireSuccessfulServerScripts();
            MinecraftForge.EVENT_BUS.post(event);
            CBJSEvents.BLAZE_BURNER_FUEL.post(ScriptType.SERVER, new BlazeBurnerFuelEventJS(event),
                    (payload, container, error) -> {
                        throw new IllegalArgumentException("Blaze Burner fuel callback failed", error);
                    });
            ScriptReloadGuard.requireSuccessfulServerScripts();
        });
    }

    static boolean reload(Consumer<BlazeBurnerFuelEvent> listener) {
        try {
            BlazeBurnerFuelEvent event = new BlazeBurnerFuelEvent();
            listener.accept(event);
            List<Rule> next = event.buildRules();
            rules = FuelSnapshot.of(next);
            CBJS.LOGGER.info("Loaded {} custom Blaze Burner fuel rules", next.size());
            return true;
        } catch (RuntimeException exception) {
            CBJS.LOGGER.error("Keeping previous Blaze Burner fuels because the new rules were invalid", exception);
            return false;
        }
    }

    /** Exact IDs win over tags; among overlapping tags the last registration wins. */
    public static Rule find(ItemStack stack) {
        FuelSnapshot snapshot = rules;
        if (snapshot == EMPTY || stack.isEmpty()) return null;
        Rule exact = snapshot.exact().get(stack.getItem());
        if (exact != null) return exact;
        for (Rule rule : snapshot.tags()) if (rule.matches(stack)) return rule;
        return null;
    }

    public static void serverStopped(ServerStoppedEvent event) {
        rules = EMPTY;
    }

    private record FuelSnapshot(Map<Item, Rule> exact, List<Rule> tags) {
        static FuelSnapshot of(List<Rule> rules) {
            if (rules.isEmpty()) return EMPTY;
            Map<Item, Rule> exact = new IdentityHashMap<>();
            List<Rule> tags = new ArrayList<>();
            for (int index = rules.size() - 1; index >= 0; index--) {
                Rule rule = rules.get(index);
                if (rule.tag() == null) exact.putIfAbsent(rule.item(), rule);
                else tags.add(rule);
            }
            return new FuelSnapshot(Collections.unmodifiableMap(exact), List.copyOf(tags));
        }
    }

    private BlazeBurnerFuelManager() {
    }
}
