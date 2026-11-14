package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.quiwh.cbjs.kinetics.CogwheelKineticState;

/** Network stress, capacity and overstressed state observed at the end of a kinetic tick. */
public final class KineticStressChangedEventJS extends KineticEventJS {
    private final CogwheelKineticState previous;
    private final CogwheelKineticState current;

    public KineticStressChangedEventJS(KineticBlockEntity entity, CogwheelKineticState previous,
                                       CogwheelKineticState current) {
        super(entity);
        this.previous = previous;
        this.current = current;
    }

    public float getOldStress() { return previous.stress(); }
    public float getStress() { return current.stress(); }
    public float getOldCapacity() { return previous.capacity(); }
    public float getCapacity() { return current.capacity(); }
    public boolean isPreviouslyOverstressed() { return previous.overstressed(); }
    public boolean isOverstressed() { return current.overstressed(); }
}
