package net.quiwh.cbjs.kinetics;

/** Saved source SU consumed when native unloaded-network bookkeeping loads this member. */
public interface SavedKineticCapacity {
    /** Returns NaN when no saved actual contribution is available. */
    float cbjs$consumeSavedCapacity(float lastCapacity);
}
