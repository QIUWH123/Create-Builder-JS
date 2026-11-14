package net.quiwh.cbjs.kubejs;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.quiwh.cbjs.fluids.PipeConnectionState;

/** Pressure at one connection; these are Create's pressure values, not mB per tick. */
public final class FluidPipePressureChangedEventJS extends FluidPipeEventJS {
    private final String side;
    private final PipeConnectionState previous;
    private final PipeConnectionState current;

    public FluidPipePressureChangedEventJS(SmartBlockEntity entity, Direction side,
                                           PipeConnectionState previous, PipeConnectionState current) {
        super(entity);
        this.side = side.getName();
        this.previous = previous;
        this.current = current;
    }

    public String getSide() { return side; }
    public float getOldInboundPressure() { return previous.inboundPressure(); }
    public float getInboundPressure() { return current.inboundPressure(); }
    public float getOldOutwardPressure() { return previous.outwardPressure(); }
    public float getOutwardPressure() { return current.outwardPressure(); }
}
