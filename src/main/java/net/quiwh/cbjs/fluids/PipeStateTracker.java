package net.quiwh.cbjs.fluids;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

/** Six local baselines, allocated only for a pipe with matching listeners. Holds no world references. */
public final class PipeStateTracker {
    private final PipeConnectionState[] states = new PipeConnectionState[6];

    public PipeStateTracker() { Arrays.fill(states, PipeConnectionState.EMPTY); }

    public PipeConnectionState current(Direction side) { return states[side.ordinal()]; }

    /** Unchanged samples reuse their snapshot, including when only source amounts change. */
    public PipeConnectionState observe(Direction side, boolean flowing, FluidStack fluid,
                                        boolean inbound, boolean complete, float inPressure, float outPressure) {
        PipeConnectionState previous = current(side);
        if (!previous.matches(flowing, fluid, inbound, complete, inPressure, outPressure)) {
            states[side.ordinal()] = new PipeConnectionState(flowing, fluid, inbound, complete, inPressure, outPressure);
        }
        return previous;
    }
}
