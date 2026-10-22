package net.quiwh.cbjs.fluids;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

/** Immutable event baseline. Source stack amount and animation progress are not throughput. */
public record PipeConnectionState(boolean flowing, FluidStack fluid, boolean inbound, boolean complete,
                                  float inboundPressure, float outwardPressure) {
    public static final PipeConnectionState EMPTY =
            new PipeConnectionState(false, FluidStack.EMPTY, false, false, 0, 0);

    public PipeConnectionState {
        flowing = flowing && fluid != null && !fluid.isEmpty();
        fluid = flowing ? fluid.copy() : FluidStack.EMPTY;
        inbound = flowing && inbound;
        complete = flowing && complete;
    }

    @Override public FluidStack fluid() { return fluid.copy(); }

    public Fluid fluidType() { return fluid.getFluid(); }

    public boolean flowChangedFrom(PipeConnectionState previous) {
        return !previous.matchesFlow(flowing, fluid, inbound, complete);
    }

    public boolean pressureChangedFrom(PipeConnectionState previous) {
        return Float.compare(inboundPressure, previous.inboundPressure) != 0
                || Float.compare(outwardPressure, previous.outwardPressure) != 0;
    }

    public boolean matches(boolean flow, FluidStack stack, boolean in, boolean done,
                           float inPressure, float outPressure) {
        return matchesFlow(flow, stack, in, done)
                && Float.compare(inboundPressure, inPressure) == 0
                && Float.compare(outwardPressure, outPressure) == 0;
    }

    private boolean matchesFlow(boolean flow, FluidStack stack, boolean in, boolean done) {
        boolean active = flow && stack != null && !stack.isEmpty();
        return flowing == active && (!active || inbound == in && complete == done && fluid.isFluidEqual(stack));
    }
}
