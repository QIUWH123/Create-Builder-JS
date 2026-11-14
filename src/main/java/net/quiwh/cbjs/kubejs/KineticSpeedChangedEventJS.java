package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.quiwh.cbjs.kinetics.CogwheelKineticState;

/** Signed actual and theoretical RPM observed at the end of a kinetic tick. */
public final class KineticSpeedChangedEventJS extends KineticEventJS {
    private final CogwheelKineticState previous;
    private final CogwheelKineticState current;

    public KineticSpeedChangedEventJS(KineticBlockEntity entity, CogwheelKineticState previous,
                                      CogwheelKineticState current) {
        super(entity);
        this.previous = previous;
        this.current = current;
    }

    public float getOldRpm() { return previous.rpm(); }
    public float getRpm() { return current.rpm(); }
    public float getOldTheoreticalRpm() { return previous.theoreticalRpm(); }
    public float getTheoreticalRpm() { return current.theoreticalRpm(); }
}
