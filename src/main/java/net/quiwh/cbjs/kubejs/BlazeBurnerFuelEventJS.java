package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent;

/** Server data reload configuration for Create's existing Blaze Burner. */
public final class BlazeBurnerFuelEventJS extends EventJS {
    private final BlazeBurnerFuelEvent event;

    public BlazeBurnerFuelEventJS(BlazeBurnerFuelEvent event) {
        this.event = event;
    }

    public BlazeBurnerFuelEventJS addFuel(String itemOrTag, double burnTicks) {
        event.addFuel(itemOrTag, burnTicks);
        return this;
    }

    public BlazeBurnerFuelEventJS addSuperFuel(String itemOrTag, double burnTicks) {
        event.addSuperFuel(itemOrTag, burnTicks);
        return this;
    }
}
