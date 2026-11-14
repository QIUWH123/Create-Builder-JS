package net.quiwh.cbjs.kinetics;

import java.util.Objects;

/** Owned by one observer; contains no entity or world references. */
public final class CogwheelStateTracker {
    private CogwheelKineticState current = CogwheelKineticState.ZERO;
    private boolean needsObservation = true;

    /** Mutation hooks only request a sample; callbacks remain at the end of the kinetic tick. */
    public void markDirty() {
        needsObservation = true;
    }

    public boolean needsObservation() {
        return needsObservation;
    }

    /** Advances before returning, so callbacks see the updated baseline even during reentry. */
    public CogwheelKineticState observe(CogwheelKineticState next) {
        Objects.requireNonNull(next, "next kinetic state");
        needsObservation = false;
        CogwheelKineticState previous = current;
        if (next.speedChangedFrom(previous) || next.stressChangedFrom(previous)) current = next;
        return previous;
    }

    /** Unchanged ticks retain the immutable snapshot without constructing a replacement. */
    public CogwheelKineticState observe(float rpm, float theoreticalRpm, float stress, float capacity,
                                        boolean overstressed) {
        needsObservation = false;
        CogwheelKineticState previous = current;
        if (!previous.matches(rpm, theoreticalRpm, stress, capacity, overstressed)) {
            current = new CogwheelKineticState(rpm, theoreticalRpm, stress, capacity, overstressed);
        }
        return previous;
    }

    public CogwheelKineticState current() {
        return current;
    }
}
