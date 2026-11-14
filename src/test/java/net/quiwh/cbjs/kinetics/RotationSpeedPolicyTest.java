package net.quiwh.cbjs.kinetics;

/** Checks which destination is protected by Create's global speed cap. */
public final class RotationSpeedPolicyTest {
    private static int checks;

    public static void main(String[] args) {
        expect(false, 256, 256, false, "ordinary gear at global limit");
        expect(false, -256, 256, false, "ordinary reverse gear at global limit");
        expect(true, 257, 256, false, "ordinary gear above global limit");
        expect(true, -257, 256, false, "ordinary reverse gear above global limit");
        expect(false, 512, 256, true, "custom gear above global limit");
        expect(false, -512, 256, true, "custom reverse gear above global limit");
        expect(false, 1024, 64, true, "custom gear independent of lowered global cap");
        expect(false, 1024, 2048, true, "custom gear independent of raised global cap");
        expect(true, 1024, 64, false, "ordinary gear uses lowered global cap");
        expect(false, 1024, 2048, false, "ordinary gear uses raised global cap");

        KineticLimits material = new KineticLimits(512.0, 8192.0);
        if (material.isExceeded(-512, 8192) || !material.isExceeded(513, 0)
                || !material.isExceeded(-513, 0) || !material.isExceeded(0, 8193)) {
            throw new AssertionError("Independent gear RPM and stress limits must still apply");
        }
        if (new KineticLimits(null, null).isExceeded(4096, 0)) {
            throw new AssertionError("Null material RPM must remain unlimited");
        }
        System.out.println("RotationSpeedPolicyTest: " + checks + " global cap checks and material limits passed");
    }

    private static void expect(boolean expected, float speed, int globalCap, boolean material, String description) {
        checks++;
        boolean actual = RotationSpeedPolicy.exceedsCreateLimit(speed, globalCap, material);
        if (actual != expected) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
