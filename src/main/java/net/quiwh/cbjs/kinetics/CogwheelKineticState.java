package net.quiwh.cbjs.kinetics;

/** Immutable observed kinetic values; actual and theoretical RPM retain their direction. */
public record CogwheelKineticState(float rpm, float theoreticalRpm, float stress, float capacity,
                                  boolean overstressed) {
    public static final CogwheelKineticState ZERO = new CogwheelKineticState(0, 0, 0, 0, false);

    public boolean speedChangedFrom(CogwheelKineticState previous) {
        return changed(rpm, previous.rpm) || changed(theoreticalRpm, previous.theoreticalRpm);
    }

    public boolean stressChangedFrom(CogwheelKineticState previous) {
        return changed(stress, previous.stress) || changed(capacity, previous.capacity)
                || overstressed != previous.overstressed;
    }

    public boolean matches(float rpm, float theoreticalRpm, float stress, float capacity, boolean overstressed) {
        return !changed(this.rpm, rpm) && !changed(this.theoreticalRpm, theoreticalRpm)
                && !changed(this.stress, stress) && !changed(this.capacity, capacity)
                && this.overstressed == overstressed;
    }

    private static boolean changed(float value, float previous) {
        // Numeric equality intentionally treats +0 and -0 alike; repeated NaN is also stable.
        return value != previous && !(Float.isNaN(value) && Float.isNaN(previous));
    }
}
