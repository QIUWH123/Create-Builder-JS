package net.quiwh.cbjs.kinetics;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/** One demand result shared by a network's limit checks until its inputs change. */
public final class NetworkStressSnapshot {
    private final DoubleSupplier calculation;
    private boolean valid;
    private float demand;

    public NetworkStressSnapshot(DoubleSupplier calculation) {
        this.calculation = Objects.requireNonNull(calculation);
    }

    public float get() {
        if (!valid) record((float) calculation.getAsDouble());
        return demand;
    }

    public void invalidate() {
        valid = false;
    }

    /** Native Create recalculations also satisfy pending Create Builder JS checks. */
    public void record(float demand) {
        this.demand = demand;
        valid = true;
    }
}
