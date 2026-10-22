package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Wrapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Container/content selectors retained inside native KubeJS listener containers. */
public final class StorageContentFilters {
    private static Plan fluidPlan;
    private static Plan inventoryPlan;

    private StorageContentFilters() {}

    public static boolean handles(EventHandler event) {
        return event == CBJSEvents.STORAGE_FLUID_CHANGED || event == CBJSEvents.STORAGE_INVENTORY_CHANGED;
    }

    public static Object[] arguments(EventHandler event, Object[] arguments) {
        if (!handles(event)) return arguments;
        // Parse in listen(), inside KubeJS's registration error handler.
        if (arguments.length == 3) {
            return new Object[] {new Registration(arguments[0], arguments[1]), arguments[2]};
        }
        // A list is one OR selector; native KubeJS would split it into several listeners.
        if (arguments.length == 2 && (RegistrySelector.isList(arguments[0])
                || RegistrySelector.scalarValue(arguments[0]) == null)) {
            return new Object[] {new Registration(arguments[0], null), arguments[1]};
        }
        return arguments;
    }

    public static IEventHandler filteredHandler(EventHandler event, Object extraId, IEventHandler callback) {
        if (!handles(event)) return null;
        Object value = selectorValue(extraId);
        Filter filter;
        if (value instanceof Registration registration) {
            filter = filter(event, registration.block, registration.content);
        } else if (RegistrySelector.isList(value)
                || value instanceof String text && (text.startsWith("#") || text.equals("*"))) {
            filter = filter(event, value, null);
        } else {
            // Validate supplied scalars before native Extra.ID can convert malformed values
            // into null/global listeners. Valid IDs retain native keyed dispatch.
            RegistrySelector.parse(value, Registries.BLOCK, "storage container");
            return null;
        }
        return new FilteredHandler(filter, callback);
    }

    private static Filter filter(EventHandler event, Object block, Object content) {
        var container = RegistrySelector.parse(block, Registries.BLOCK, "storage container");
        return event == CBJSEvents.STORAGE_FLUID_CHANGED
                ? new Filter(container, RegistrySelector.parse(content, Registries.FLUID, "storage fluid"), null)
                : new Filter(container, null, RegistrySelector.parse(content, Registries.ITEM, "storage item"));
    }

    private static Object selectorValue(Object value) {
        Object unwrapped = Wrapper.unwrapped(value);
        return unwrapped instanceof CharSequence text ? text.toString() : unwrapped;
    }

    public static void invalidate(EventHandler event, ScriptType type) {
        if (type != ScriptType.SERVER) return;
        if (event == CBJSEvents.STORAGE_FLUID_CHANGED) fluidPlan = null;
        else if (event == CBJSEvents.STORAGE_INVENTORY_CHANGED) inventoryPlan = null;
    }

    /** Reject unrelated containers before a mutator copies any old stack or queues a slot. */
    public static boolean hasListeners(EventHandler event, Block block) {
        if (!event.hasListeners()) return false;
        if (!handles(event)) return event.hasListeners(ForgeRegistries.BLOCKS.getKey(block));
        Plan plan = plan(event);
        if (plan.unfiltered || plan.exactBlocks.contains(ForgeRegistries.BLOCKS.getKey(block))) return true;
        for (RegistrySelector<Block> selector : plan.blocks) {
            if (selector.matches(block.builtInRegistryHolder())) return true;
        }
        return false;
    }

    private static Plan plan(EventHandler event) {
        Plan plan = event == CBJSEvents.STORAGE_FLUID_CHANGED ? fluidPlan : inventoryPlan;
        if (plan == null) {
            var exact = new HashSet<ResourceLocation>();
            var selectors = new ArrayList<RegistrySelector<Block>>();
            boolean[] unfiltered = {false};
            event.forEachListener(ScriptType.SERVER, container -> {
                if (container.handler instanceof FilteredHandler handler) {
                    if (!handler.filter.isEmpty()) selectors.add(handler.filter.block);
                }
                else if (container.extraId instanceof ResourceLocation id) exact.add(id);
                else if (container.extraId == null) unfiltered[0] = true;
            });
            plan = new Plan(Set.copyOf(exact), java.util.List.copyOf(selectors), unfiltered[0]);
            if (event == CBJSEvents.STORAGE_FLUID_CHANGED) fluidPlan = plan;
            else inventoryPlan = plan;
        }
        return plan;
    }

    private record Registration(Object block, Object content) {}
    private record Plan(Set<ResourceLocation> exactBlocks, java.util.List<RegistrySelector<Block>> blocks, boolean unfiltered) {}

    private record Filter(RegistrySelector<Block> block, RegistrySelector<Fluid> fluid, RegistrySelector<Item> item) {
        boolean isEmpty() {
            return block.isEmpty() || fluid != null && fluid.isEmpty() || item != null && item.isEmpty();
        }

        boolean matches(EventJS event) {
            if (!(event instanceof StorageEventJS storage)
                    || !block.matches(storage.filterBlock().builtInRegistryHolder())) return false;
            if (event instanceof StorageFluidChangedEventJS change && fluid != null) {
                return matches(change.filterOldFluid()) || matches(change.filterFluid());
            }
            if (event instanceof StorageInventoryChangedEventJS change && item != null) {
                // The batch stays intact; one matching changed slot selects the callback once.
                for (StorageSlotChange slot : change.getChanges()) {
                    if (matches(slot.filterOldItem()) || matches(slot.filterItem())) return true;
                }
            }
            return false;
        }

        private boolean matches(FluidStack stack) {
            return !stack.isEmpty() && fluid.matches(stack.getFluid().builtInRegistryHolder());
        }

        private boolean matches(ItemStack stack) {
            return !stack.isEmpty() && item.matches(stack.getItem().builtInRegistryHolder());
        }
    }

    private record FilteredHandler(Filter filter, IEventHandler callback) implements IEventHandler {
        @Override public Object onEvent(EventJS event) throws EventExit {
            return filter.matches(event) ? callback.onEvent(event) : null;
        }
    }

}
