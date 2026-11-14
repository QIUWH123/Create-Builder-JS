package net.quiwh.cbjs.content;

/** Immutable connectivity payload; each split member retains the complete six-face preference. */
public record FluidVesselWindowData(FluidWindowFaces faces) {
    public boolean enabled() { return faces.anyGlass(); }
}
