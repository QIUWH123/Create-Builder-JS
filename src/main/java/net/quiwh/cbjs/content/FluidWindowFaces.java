package net.quiwh.cbjs.content;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;

/** Immutable world-facing window settings; safe to share between controller and members. */
public final class FluidWindowFaces {
    private final Map<Direction, FluidWindowMode> modes;

    private FluidWindowFaces(Map<Direction, FluidWindowMode> modes) { this.modes = Map.copyOf(modes); }

    public static FluidWindowFaces filled(FluidWindowMode mode) {
        Objects.requireNonNull(mode, "mode");
        EnumMap<Direction, FluidWindowMode> modes = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) modes.put(face, mode);
        return new FluidWindowFaces(modes);
    }

    public FluidWindowMode get(Direction face) { return modes.get(Objects.requireNonNull(face, "face")); }

    public FluidWindowFaces with(Direction face, FluidWindowMode mode) {
        Objects.requireNonNull(face, "face"); Objects.requireNonNull(mode, "mode");
        if (get(face) == mode) return this;
        EnumMap<Direction, FluidWindowMode> changed = new EnumMap<>(Direction.class);
        changed.putAll(modes); changed.put(face, mode);
        return new FluidWindowFaces(changed);
    }

    public boolean anyGlass() { return modes.values().stream().anyMatch(mode -> mode != FluidWindowMode.NONE); }

    public FluidWindowFaces transform(UnaryOperator<Direction> transform) {
        EnumMap<Direction, FluidWindowMode> changed = new EnumMap<>(Direction.class);
        for (Direction face : Direction.values()) changed.put(transform.apply(face), get(face));
        if (changed.size() != Direction.values().length) throw new IllegalArgumentException("Face transform must be a permutation");
        return new FluidWindowFaces(changed);
    }

    @Override public boolean equals(Object other) { return other instanceof FluidWindowFaces faces && modes.equals(faces.modes); }
    @Override public int hashCode() { return modes.hashCode(); }
    @Override public String toString() { return modes.toString(); }
}
