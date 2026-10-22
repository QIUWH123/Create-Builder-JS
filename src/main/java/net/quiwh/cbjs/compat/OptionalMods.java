package net.quiwh.cbjs.compat;

import net.minecraftforge.fml.ModList;

/** Safe entry points keep optional addon classes out of ordinary startup. */
public final class OptionalMods {
    public static boolean bitsAndBobsLoaded() {
        ModList mods = ModList.get();
        return mods != null && mods.isLoaded("bits_n_bobs");
    }
    private OptionalMods() { }
}
