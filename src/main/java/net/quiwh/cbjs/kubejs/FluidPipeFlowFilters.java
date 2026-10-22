package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import dev.latvian.mods.kubejs.event.EventExit;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Extends native KubeJS listeners while keeping callbacks, errors and reloads in its event containers. */
public final class FluidPipeFlowFilters {
    // Only selector values are retained here; script callbacks belong to the native event containers.
    private static Plan plan;

    private FluidPipeFlowFilters() {}

    public static boolean isFlowEvent(EventHandler event) {
        return event.name.equals("fluidPipeFlow") && event == CBJSEvents.FLUID_PIPE_FLOW;
    }

    public static Object[] arguments(EventHandler event, Object[] arguments) {
        if (!isFlowEvent(event)) return arguments;
        if (arguments.length == 2 && (RegistrySelector.isList(arguments[0])
                || RegistrySelector.scalarValue(arguments[0]) == null))
            return new Object[] {new SingleRegistration(arguments[0]), arguments[1]};
        if (arguments.length != 3) return arguments;
        // Parse in listen(), inside KubeJS's registration error handler.
        return new Object[] {new Registration(arguments[0], arguments[1]), arguments[2]};
    }

    public static IEventHandler filteredHandler(Object extraId, IEventHandler callback) {
        Object value = Wrapper.unwrapped(extraId);
        FluidPipeFlowFilter selector;
        if (value instanceof SingleRegistration registration) {
            selector = FluidPipeFlowFilter.either(registration.selector);
        } else if (value instanceof Registration registration) {
            selector = FluidPipeFlowFilter.of(selectorValue(registration.fluid), selectorValue(registration.block));
        } else if (value instanceof FluidPipeFlowFilter filter) {
            selector = filter;
        } else if (value instanceof CharSequence string
                && (string.toString().startsWith("#") || string.toString().equals("*"))) {
            selector = FluidPipeFlowFilter.either(string.toString());
        } else {
            Object single = selectorValue(value);
            RegistrySelector.parse(single, net.minecraft.core.registries.Registries.FLUID,
                    "fluidPipeFlow fluid or block");
            ResourceLocation id = single instanceof ResourceLocation location ? location
                    : single instanceof String text ? ResourceLocation.tryParse(text) : null;
            // Preserve native fluid-only listeners (and their ordering) for fluid IDs such as water.
            // Server scripts run after custom blocks have been registered.
            if (id == null || ForgeRegistries.FLUIDS.containsKey(id) || !ForgeRegistries.BLOCKS.containsKey(id))
                return null;
            selector = FluidPipeFlowFilter.of(null, id);
        }
        return new FilteredHandler(selector, callback);
    }

    private static Object selectorValue(Object value) {
        Object unwrapped = Wrapper.unwrapped(value);
        return unwrapped instanceof CharSequence string ? string.toString() : unwrapped;
    }

    public static void invalidate(EventHandler event, ScriptType type) {
        if (type == ScriptType.SERVER && isFlowEvent(event)) plan = null;
    }

    /** The first gate can reject unrelated blocks before reading a pipe's fluid connections. */
    public static boolean hasListeners(Block block) {
        if (!CBJSEvents.FLUID_PIPE_FLOW.hasListeners()) return false;
        Plan current = plan();
        if (current.unfiltered || !current.exactFluids.isEmpty()) return true;
        for (FluidPipeFlowFilter selector : current.selectors) {
            if (selector.matchesBlock(block)) return true;
        }
        return false;
    }

    /** No stacks, event payloads or tag caches are allocated for a nonmatching connection. */
    public static boolean hasListeners(Block block, Fluid fluid) {
        if (!CBJSEvents.FLUID_PIPE_FLOW.hasListeners()) return false;
        Plan current = plan();
        if (current.unfiltered || current.exactFluids.contains(ForgeRegistries.FLUIDS.getKey(fluid))) return true;
        for (FluidPipeFlowFilter selector : current.selectors) {
            if (selector.matches(block, fluid)) return true;
        }
        return false;
    }

    private static Plan plan() {
        if (plan == null) {
            var exact = new HashSet<ResourceLocation>();
            var selectors = new ArrayList<FluidPipeFlowFilter>();
            boolean[] unfiltered = {false};
            CBJSEvents.FLUID_PIPE_FLOW.forEachListener(ScriptType.SERVER, container -> {
                if (container.handler instanceof FilteredHandler handler) {
                    selectors.add(handler.filter);
                } else if (container.extraId instanceof ResourceLocation id) {
                    exact.add(id);
                } else if (container.extraId == null) {
                    unfiltered[0] = true;
                }
            });
            plan = new Plan(Set.copyOf(exact), selectors.toArray(FluidPipeFlowFilter[]::new), unfiltered[0]);
        }
        return plan;
    }

    private record Registration(Object fluid, Object block) {}
    private record SingleRegistration(Object selector) {}
    private record Plan(Set<ResourceLocation> exactFluids, FluidPipeFlowFilter[] selectors, boolean unfiltered) {}

    private record FilteredHandler(FluidPipeFlowFilter filter, IEventHandler callback) implements IEventHandler {
        @Override
        public Object onEvent(EventJS event) throws EventExit {
            if (event instanceof FluidPipeFlowEventJS flow
                    && filter.matches(flow.getFilterBlock(), flow.getFilterFluid())) {
                return callback.onEvent(event);
            }
            return null;
        }
    }
}
