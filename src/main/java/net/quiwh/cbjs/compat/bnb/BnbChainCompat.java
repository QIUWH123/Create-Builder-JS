package net.quiwh.cbjs.compat.bnb;

import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.compat.OptionalMods;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;

/** This entry point can be loaded without Bits 'n' Bobs on the class path. */
public final class BnbChainCompat {
    public static final String MOD_ID = "bits_n_bobs";

    public static boolean isLoaded() {
        return OptionalMods.bitsAndBobsLoaded();
    }

    public static void register() {
        if (isLoaded()) BnbChainBlockEntities.register();
    }

    public static void add(CogwheelBuilder source) {
        if (isLoaded()) BnbChainedCogwheelBuilder.add(source);
    }

    public static void onRegister(RegisterEvent event) {
        if (isLoaded()) BnbChainedCogwheelBuilder.onRegister(event);
    }

    private BnbChainCompat() { }
}
