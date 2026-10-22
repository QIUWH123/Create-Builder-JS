package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Paired belt/item OR selectors; both selectors must match the completed action. */
public final class BeltEventFilters {
    private static final Map<EventHandler, Plan> PLANS = new HashMap<>();
    private BeltEventFilters() { }
    public static boolean handles(EventHandler event) {
        return event == CBJSEvents.BELT_ITEM_ENTERED || event == CBJSEvents.BELT_ITEM_EXITED
                || event == CBJSEvents.BELT_ITEM_PROCESSED;
    }
    public static Object[] arguments(EventHandler event, Object[] arguments) {
        if (!handles(event)) return arguments;
        if (arguments.length == 3)
            return new Object[]{new Registration(arguments[0], arguments[1]), arguments[2]};
        if (arguments.length == 2 && (RegistrySelector.isList(arguments[0])
                || RegistrySelector.scalarValue(arguments[0]) == null))
            return new Object[]{new Registration(arguments[0], null), arguments[1]};
        return arguments;
    }
    public static IEventHandler filteredHandler(EventHandler event, Object extraId, IEventHandler callback) {
        if (!handles(event)) return null;
        Object value = RegistrySelector.scalarValue(extraId);
        if (value instanceof Registration registration)
            return new FilteredHandler(filter(registration.block, registration.item), callback);
        if (RegistrySelector.isList(value) || value instanceof String text && (text.startsWith("#") || text.equals("*")))
            return new FilteredHandler(filter(value, null), callback);
        RegistrySelector.parse(value, Registries.BLOCK, "belt block");
        return null;
    }
    private static Filter filter(Object block, Object item) {
        return new Filter(RegistrySelector.parse(block, Registries.BLOCK, "belt block"),
                RegistrySelector.parse(item, Registries.ITEM, "belt item"));
    }
    public static void invalidate(EventHandler event, ScriptType type) {
        if (type == ScriptType.SERVER && handles(event)) PLANS.remove(event);
    }
    public static boolean hasListeners(EventHandler event, Block block) {
        if (!event.hasListeners()) return false;
        Plan plan = PLANS.computeIfAbsent(event, BeltEventFilters::plan);
        if (plan.unfiltered || plan.exact.contains(ForgeRegistries.BLOCKS.getKey(block))) return true;
        for (RegistrySelector<Block> selector : plan.blocks)
            if (selector.matches(block.builtInRegistryHolder())) return true;
        return false;
    }
    private static Plan plan(EventHandler event) {
        Set<ResourceLocation> exact = new HashSet<>();
        List<RegistrySelector<Block>> selectors = new ArrayList<>();
        boolean[] unfiltered = {false};
        event.forEachListener(ScriptType.SERVER, container -> {
            if (container.handler instanceof FilteredHandler handler) {
                if (!handler.filter.isEmpty()) selectors.add(handler.filter.block);
            } else if (container.extraId instanceof ResourceLocation id) exact.add(id);
            else if (container.extraId == null) unfiltered[0] = true;
        });
        return new Plan(Set.copyOf(exact), List.copyOf(selectors), unfiltered[0]);
    }
    private record Registration(Object block, Object item) { }
    private record Plan(Set<ResourceLocation> exact, List<RegistrySelector<Block>> blocks, boolean unfiltered) { }
    private record Filter(RegistrySelector<Block> block, RegistrySelector<Item> item) {
        boolean isEmpty() { return block.isEmpty() || item.isEmpty(); }
        boolean matches(EventJS event) {
            return event instanceof BeltItemEventJS action
                    && block.matches(action.filterBlock().builtInRegistryHolder()) && action.matchesItem(item);
        }
    }
    private record FilteredHandler(Filter filter, IEventHandler callback) implements IEventHandler {
        @Override public Object onEvent(EventJS event) throws EventExit {
            return filter.matches(event) ? callback.onEvent(event) : null;
        }
    }
}
