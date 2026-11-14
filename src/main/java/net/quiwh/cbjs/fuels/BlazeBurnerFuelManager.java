package net.quiwh.cbjs.fuels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventHandlerContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Configuration;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Layer;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Rule;
import net.quiwh.cbjs.kinetics.ScriptReloadGuard;
import net.quiwh.cbjs.kubejs.BlazeBurnerFuelEventJS;
import net.quiwh.cbjs.kubejs.BurnerFuelEventJS;
import net.quiwh.cbjs.kubejs.CBJSEvents;

/** Publishes global and per-block fuel snapshots atomically without changing native fuel tags. */
public final class BlazeBurnerFuelManager {
    private static final FuelSnapshot EMPTY_FUELS = new FuelSnapshot(Map.of(), List.of());
    private static final Snapshot EMPTY = new Snapshot(List.of(), Map.of());
    private static volatile Snapshot rules = EMPTY;

    public static void addReloadListener(AddReloadListenerEvent event) {
        event.addListener((barrier, resources, preparationProfiler, applyProfiler, preparationExecutor, gameExecutor) ->
                CompletableFuture.completedFuture(null).thenCompose(barrier::wait)
                        .thenRunAsync(BlazeBurnerFuelManager::reload, gameExecutor));
    }

    public static void reload() {
        reload(event -> {
            ScriptReloadGuard.requireSuccessfulServerScripts();
            requireSuccessfulStartupScripts();
            dispatch(CBJSEvents.BURNER_FUEL, ScriptType.STARTUP, event);
            event.beginLayer();
            NeoForge.EVENT_BUS.post(event);
            dispatch(CBJSEvents.BURNER_FUEL, ScriptType.SERVER, event);
            CBJSEvents.BLAZE_BURNER_FUEL.post(ScriptType.SERVER, new BlazeBurnerFuelEventJS(event));
            requireSuccessfulStartupScripts();
            ScriptReloadGuard.requireSuccessfulServerScripts();
        });
    }

    private static void dispatch(EventHandler handler, ScriptType type, BlazeBurnerFuelEvent event) {
        // Dispatch each documented script type exactly once, preserving startup/server priority layers.
        List<EventHandlerContainer> listeners = new ArrayList<>();
        handler.forEachListener(type, listeners::add);
        BurnerFuelEventJS payload = new BurnerFuelEventJS(event);
        for (EventHandlerContainer listener : listeners) {
            try {
                listener.handler.onEvent(payload);
            } catch (EventExit exit) {
                if (exit.result.value() instanceof Throwable failure)
                    throw new IllegalArgumentException("Burner fuel callback failed at " + listener, failure);
                break;
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Burner fuel callback failed at " + listener, exception);
            }
        }
    }

    private static void requireSuccessfulStartupScripts() {
        // Startup callbacks survive server-script reloads, so their load errors must not publish partial defaults.
        if (!ScriptType.STARTUP.console.errors.isEmpty())
            throw new IllegalStateException("KubeJS reported startup script errors; keeping previous burner fuels");
    }

    static boolean reload(Consumer<BlazeBurnerFuelEvent> listener) {
        try {
            BlazeBurnerFuelEvent event = new BlazeBurnerFuelEvent(BurnerFuelDefaults.buildProfiles());
            listener.accept(event);
            rules = Snapshot.of(event.buildConfiguration());
            CBJS.LOGGER.info("Loaded burner fuel rules for {} configured blocks", rules.profiles().size());
            return true;
        } catch (RuntimeException exception) {
            CBJS.LOGGER.error("Keeping previous burner fuels because the new rules were invalid", exception);
            return false;
        }
    }

    /** Compatibility lookup for shared fuel rules without a per-block context. */
    public static Rule find(ItemStack stack) { return find((Block) null, stack); }

    /** Higher-priority block layers precede shared layers; exact IDs beat tags within each layer. */
    public static Rule find(Block block, ItemStack stack) {
        Snapshot snapshot = rules;
        if (snapshot == EMPTY || stack.isEmpty()) return null;
        ProfileSnapshot profile = snapshot.profiles().get(block);
        if (profile != null) {
            Rule selected = find(profile.layers(), stack);
            if (selected != null) return selected;
            if (!profile.globalFuels()) return null;
        }
        return find(snapshot.global(), stack);
    }

    public static boolean usesVanillaFuels(Block block) {
        ProfileSnapshot profile = rules.profiles().get(block);
        return profile == null || profile.vanillaFuels();
    }

    private static Rule find(List<FuelSnapshot> layers, ItemStack stack) {
        for (int index = layers.size() - 1; index >= 0; index--) {
            Rule selected = layers.get(index).find(stack);
            if (selected != null) return selected;
        }
        return null;
    }

    public static void serverStopped(ServerStoppedEvent event) { rules = EMPTY; }

    private record Snapshot(List<FuelSnapshot> global, Map<Block, ProfileSnapshot> profiles) {
        static Snapshot of(Configuration configuration) {
            Map<Block, ProfileSnapshot> profiles = new IdentityHashMap<>();
            configuration.profiles().forEach((block, layers) -> profiles.put(block, ProfileSnapshot.of(layers)));
            return new Snapshot(configuration.global().stream().map(layer -> FuelSnapshot.of(layer.rules())).toList(),
                    Collections.unmodifiableMap(profiles));
        }
    }

    private record ProfileSnapshot(List<FuelSnapshot> layers, boolean vanillaFuels, boolean globalFuels) {
        static ProfileSnapshot of(List<Layer> layers) {
            boolean vanilla = true, global = true;
            for (Layer layer : layers) {
                if (layer.vanillaFuels() != null) vanilla = layer.vanillaFuels();
                if (layer.globalFuels() != null) global = layer.globalFuels();
            }
            return new ProfileSnapshot(layers.stream().map(layer -> FuelSnapshot.of(layer.rules())).toList(), vanilla, global);
        }
    }

    private record FuelSnapshot(Map<Item, Rule> exact, List<Rule> tags) {
        static FuelSnapshot of(List<Rule> rules) {
            if (rules.isEmpty()) return EMPTY_FUELS;
            Map<Item, Rule> exact = new IdentityHashMap<>();
            List<Rule> tags = new ArrayList<>();
            for (int index = rules.size() - 1; index >= 0; index--) {
                Rule rule = rules.get(index);
                if (rule.tag() == null) exact.putIfAbsent(rule.item(), rule);
                else tags.add(rule);
            }
            return new FuelSnapshot(Collections.unmodifiableMap(exact), List.copyOf(tags));
        }
        Rule find(ItemStack stack) {
            if (this == EMPTY_FUELS) return null;
            Rule exactRule = exact.get(stack.getItem());
            if (exactRule != null) return exactRule;
            for (Rule rule : tags) if (rule.matches(stack)) return rule;
            return null;
        }
    }

    private BlazeBurnerFuelManager() { }
}
