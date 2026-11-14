package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.BurnerFuelModifier;

/** Global fuel declarations and per-burner defaults/overrides in startup or server scripts. */
public final class BurnerFuelEventJS implements KubeEvent {
    private final BlazeBurnerFuelEvent event;
    public BurnerFuelEventJS(BlazeBurnerFuelEvent event) { this.event = event; }
    public BurnerFuelEventJS addFuel(String itemOrTag, double burnTicks) { event.addFuel(itemOrTag, burnTicks); return this; }
    public BurnerFuelEventJS addSuperFuel(String itemOrTag, double burnTicks) { event.addSuperFuel(itemOrTag, burnTicks); return this; }
    public BurnerFuelModifier modifyBurner(String blockId) { return event.modifyBurner(blockId); }
}
