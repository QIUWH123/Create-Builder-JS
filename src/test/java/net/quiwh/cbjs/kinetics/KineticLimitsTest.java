package net.quiwh.cbjs.kinetics;

/** Standalone behavior checks; run with Java 17 and no test dependencies. */
public final class KineticLimitsTest {
    private static int checks;

    public static void main(String[] args) {
        rpmUsesMagnitudeAndPermitsEquality();
        stressIsIndependentAndPermitsEquality();
        absentLimitsAreUnlimited();
        zeroLimitsPermitOnlyZero();
        invalidThresholdsAreRejected();
        largeFiniteThresholdsRemainValid();
        System.out.println("KineticLimitsTest: " + checks + " checks passed");
        RotationSpeedPolicyTest.main(args);
        CogwheelStateTrackerTest.main(args);
        NetworkStressSnapshotTest.main(args);
    }

    private static void rpmUsesMagnitudeAndPermitsEquality() {
        KineticLimits limits = new KineticLimits(32.0, null);
        expect(false, limits.isExceeded(31.0, 0.0), "RPM below limit");
        expect(false, limits.isExceeded(32.0, 0.0), "positive RPM equality");
        expect(false, limits.isExceeded(-32.0, 0.0), "negative RPM equality");
        expect(true, limits.isExceeded(32.01, 0.0), "positive RPM above limit");
        expect(true, limits.isExceeded(-32.01, 0.0), "negative RPM above limit");
    }

    private static void stressIsIndependentAndPermitsEquality() {
        KineticLimits limits = new KineticLimits(64.0, 1024.0);
        expect(false, limits.isExceeded(64.0, 1024.0), "both limits at equality");
        expect(false, limits.isExceeded(0.0, 1023.0), "stress below limit");
        expect(true, limits.isExceeded(0.0, 1024.01), "stress alone exceeds limit");
        expect(true, limits.isExceeded(-65.0, 0.0), "RPM alone exceeds limit");
        expect(true, limits.isExceeded(65.0, 1025.0), "both exceed limits");
    }

    private static void absentLimitsAreUnlimited() {
        expect(false, new KineticLimits(null, null).isExceeded(-Double.MAX_VALUE, Double.MAX_VALUE),
                "both absent limits are unlimited");
        expect(false, new KineticLimits(null, null).isExceeded(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                "both absent limits also ignore infinite observations");
        expect(false, new KineticLimits(null, 100.0).isExceeded(-Double.MAX_VALUE, 100.0),
                "absent RPM limit leaves stress limit active");
        expect(true, new KineticLimits(null, 100.0).isExceeded(0.0, 101.0),
                "present stress limit remains enforced");
        expect(false, new KineticLimits(100.0, null).isExceeded(100.0, Double.MAX_VALUE),
                "absent stress limit leaves RPM limit active");
        expect(true, new KineticLimits(100.0, null).isExceeded(-101.0, 0.0),
                "present RPM limit remains enforced");
    }

    private static void zeroLimitsPermitOnlyZero() {
        KineticLimits limits = new KineticLimits(0.0, 0.0);
        expect(false, limits.isExceeded(0.0, 0.0), "zero equality");
        expect(false, limits.isExceeded(-0.0, 0.0), "negative zero equality");
        expect(true, limits.isExceeded(Double.MIN_VALUE, 0.0), "positive RPM above zero");
        expect(true, limits.isExceeded(-Double.MIN_VALUE, 0.0), "negative RPM above zero");
        expect(true, limits.isExceeded(0.0, Double.MIN_VALUE), "stress above zero");
    }

    private static void invalidThresholdsAreRejected() {
        double[] invalid = {-Double.MIN_VALUE, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (double value : invalid) {
            expectRejected(() -> new KineticLimits(value, null), "invalid RPM threshold: " + value);
            expectRejected(() -> new KineticLimits(null, value), "invalid stress threshold: " + value);
        }
    }

    private static void largeFiniteThresholdsRemainValid() {
        KineticLimits limits = new KineticLimits(Double.MAX_VALUE, Double.MAX_VALUE);
        expect(false, limits.isExceeded(Double.MAX_VALUE, Double.MAX_VALUE), "largest finite equality");
        expect(false, limits.isExceeded(-Double.MAX_VALUE, Double.MAX_VALUE),
                "largest negative RPM equality");
        expect(true, new KineticLimits(3_000_000_000.0, null).isExceeded(3_000_000_001.0, 0.0),
                "RPM threshold exceeds signed integer range");
        expect(true, new KineticLimits(null, 3_000_000_000.0).isExceeded(0.0, 3_000_000_001.0),
                "stress threshold exceeds signed integer range");
    }

    private static void expect(boolean expected, boolean actual, String description) {
        checks++;
        if (expected != actual) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }

    private static void expectRejected(Runnable action, String description) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(description + ": expected IllegalArgumentException");
    }
}
