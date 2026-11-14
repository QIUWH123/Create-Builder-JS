package net.quiwh.cbjs.kinetics;

/** Active kinetic ratings are enforced by GearLimitManager instead of Create's global cap. */
public final class RotationSpeedPolicy {
    public static boolean exceedsCreateLimit(float speed, int globalMaxRpm, boolean independentRpmLimit) {
        return !independentRpmLimit && Math.abs(speed) > globalMaxRpm;
    }

    private RotationSpeedPolicy() {
    }
}
