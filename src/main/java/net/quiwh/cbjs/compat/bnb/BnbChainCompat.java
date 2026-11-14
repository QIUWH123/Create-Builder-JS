package net.quiwh.cbjs.compat.bnb;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.compat.OptionalMods;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;

/** This entry point can be loaded without Bits 'n' Bobs on the class path. */
public final class BnbChainCompat {
    public static final String MOD_ID = "bits_n_bobs";

    public static boolean isLoaded() {
        return OptionalMods.bitsAndBobsLoaded();
    }

    /** BnB's Azimuth applicator supplies the native behaviour; no companion BE is needed. */
    public static void register() { }

    public static void add(CogwheelBuilder source) {
        // BnB2.3 attaches to the original cogwheel. Its candidate hook reads this public flag.
        if (isLoaded() && !source.anyDriveEnabled())
            throw new IllegalArgumentException("Only exposed drive-enabled cogwheels may join a drive");
    }

    public static void onRegister(RegisterEvent event) {
        if (isLoaded()) BnbNativeChainTypes.onRegister(event);
    }

    private BnbChainCompat() { }
}
