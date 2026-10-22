package net.quiwh.cbjs.content;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/** Aperture applied to one complete outer face of a tank multiblock. */
public enum FluidWindowMode implements StringRepresentable {
    FULL, NONE, STRIP;

    @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }

    public static FluidWindowMode parse(String name) {
        if (name != null) for (FluidWindowMode mode : values())
            if (mode.getSerializedName().equals(name.toLowerCase(Locale.ROOT))) return mode;
        throw new IllegalArgumentException("Unknown fluid window mode '" + name + "'; expected full, none, or strip");
    }
}
