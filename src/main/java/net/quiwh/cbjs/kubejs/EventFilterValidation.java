package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Validates this mod's exact-ID extras before KubeJS can turn invalid IDs into globals. */
public final class EventFilterValidation {
    private EventFilterValidation() {}

    private static boolean handles(EventHandler event) {
        return event.group == CBJSEvents.GROUP && event.extra == Extra.ID
                && !StorageContentFilters.handles(event) && !FluidPipeFlowFilters.isFlowEvent(event)
                && !BeltEventFilters.handles(event);
    }

    public static Object[] arguments(EventHandler event, Object[] arguments) {
        if (handles(event) && arguments.length == 2 && RegistrySelector.isList(arguments[0])) {
            // Validate the whole list inside listen(), before installing any of its listeners.
            return new Object[] {new Registration(arguments[0]), arguments[1]};
        }
        return arguments;
    }

    /** Null means that the event uses a content selector or belongs to another mod. */
    public static List<?> exactExtras(EventHandler event, Object extraId) {
        if (!handles(event)) return null;
        Object value = RegistrySelector.scalarValue(extraId);
        if (value instanceof Registration registration) {
            var extras = new LinkedHashSet<ResourceLocation>();
            for (Object member : RegistrySelector.values(registration.selectors)) {
                extras.add(exactId(event, member, false));
            }
            // Validate every member before collapsing: a wildcard must not hide a bad ID.
            return extras.contains(null) ? Collections.singletonList(null) : List.copyOf(extras);
        }
        return Collections.singletonList(exactId(event, value, true));
    }

    private static ResourceLocation exactId(EventHandler event, Object value, boolean allowNull) {
        Object scalar = RegistrySelector.scalarValue(value);
        if (scalar == null && allowNull || "*".equals(scalar)) return null;
        ResourceLocation id = scalar instanceof ResourceLocation location ? location
                : scalar instanceof String text ? ResourceLocation.tryParse(text) : null;
        if (id == null || id.getPath().isEmpty()) {
            throw new IllegalArgumentException("Invalid " + event.name + " block selector '" + scalar
                    + "': expected a block ID or *");
        }
        return id;
    }

    private record Registration(Object selectors) {}
}
