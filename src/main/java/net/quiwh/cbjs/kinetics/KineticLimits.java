package net.quiwh.cbjs.kinetics;

/** Optional limits for actual RPM and total connected network stress demand. */
public record KineticLimits(Double maxRpm, Double maxNetworkStress) {
    public KineticLimits {
        validateLimit(maxRpm, "maxRpm");
        validateLimit(maxNetworkStress, "maxNetworkStress");
    }

    /** Null limits are unlimited; reaching a limit exactly is permitted. */
    public boolean isExceeded(double rpm, double networkStress) {
        return isExceeded(rpm, networkStress,
                maxRpm == null ? Double.POSITIVE_INFINITY : maxRpm,
                maxNetworkStress == null ? Double.POSITIVE_INFINITY : maxNetworkStress);
    }

    /** Compares validated limits without allocating; positive infinity represents an absent limit. */
    public static boolean isExceeded(double rpm, double networkStress, double maxRpm, double maxNetworkStress) {
        return Math.abs(rpm) > maxRpm || networkStress > maxNetworkStress;
    }

    private static void validateLimit(Double limit, String name) {
        if (limit != null && (!Double.isFinite(limit) || limit < 0.0)) {
            throw new IllegalArgumentException(name + " must be finite and nonnegative, or null");
        }
    }
}
