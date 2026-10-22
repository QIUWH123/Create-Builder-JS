package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

/** Cancelling skips the pending material-limit destruction, including native belt teardown. */
public final class KineticOverloadEventJS extends KineticEventJS {
    private final float rpm;
    private final double stress;
    private final Integer maxRpm;
    private final Integer maxStress;

    public KineticOverloadEventJS(KineticBlockEntity entity, float rpm, double stress,
                                  Integer maxRpm, Integer maxStress) {
        super(entity);
        this.rpm = rpm;
        this.stress = stress;
        this.maxRpm = maxRpm;
        this.maxStress = maxStress;
    }

    public float getRpm() { return rpm; }
    public double getStress() { return stress; }
    public Integer getMaxRpm() { return maxRpm; }
    public Integer getMaxStress() { return maxStress; }
    public boolean isRpmExceeded() { return maxRpm != null && Math.abs(rpm) > maxRpm; }
    public boolean isStressExceeded() { return maxStress != null && stress > maxStress; }
    public String getReason() { return isRpmExceeded() ? isStressExceeded() ? "both" : "rpm" : "stress"; }
}
