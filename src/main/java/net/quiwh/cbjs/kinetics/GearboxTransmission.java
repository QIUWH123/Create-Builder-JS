package net.quiwh.cbjs.kinetics;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.quiwh.cbjs.kubejs.GearboxBuilder.Kind;

/** Original transmission policy, expressed as the speed of each port relative to one internal drive. */
public final class GearboxTransmission {
    public static boolean hasPort(Kind kind, Axis boxAxis, Direction face) {
        return kind == Kind.SIX_WAY;
    }

    /**
     * Create stores the RPM of the current input shaft. Dividing port weights keeps
     * every input/output pair reciprocal, including a network that changes source.
     */
    public static float modifier(Kind kind, Axis boxAxis, @Nullable Direction source, Direction output) {
        if (source == null) return 1;
        if (!hasPort(kind, boxAxis, source) || !hasPort(kind, boxAxis, output)) return 0;
        return portWeight(boxAxis, output) / portWeight(boxAxis, source);
    }

    private static float portWeight(Axis boxAxis, Direction face) {
        // The canonical Y model has X=-1, Y=-1 and Z=+1.
        // X uses blockstate x=90,y=90; Z uses x=90,y=180. These coefficients
        // include the resulting changes of global shaft axis and rotation sign.
        Axis shaftAxis = face.getAxis();
        float weight = switch (boxAxis) {
            case X -> shaftAxis == Axis.Y ? 1 : -1;
            case Y -> shaftAxis == Axis.Z ? 1 : -1;
            case Z -> shaftAxis == Axis.Y ? 1 : -1;
        };
        return weight * face.getAxisDirection().getStep();
    }

    private GearboxTransmission() { }
}
