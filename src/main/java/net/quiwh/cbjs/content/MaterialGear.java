package net.quiwh.cbjs.content;

/** Shared appearance metadata for both exposed and encased gears. */
public interface MaterialGear {
    GearModelSet models();

    boolean isLargeCog();

    default boolean isShaftless() {
        return false;
    }

    default boolean isEncased() {
        return false;
    }
}
