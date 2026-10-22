package net.quiwh.cbjs.kubejs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/** Cancelling skips Create's pending reaction between fluids inside this pipe. */
public final class FluidPipeCollisionEventJS extends FluidPipeEventJS {
    private final FluidStack fluid;
    private final FluidStack otherFluid;

    public FluidPipeCollisionEventJS(Level level, BlockPos pos, FluidStack fluid, FluidStack otherFluid) {
        super(level, pos, level.getBlockState(pos));
        this.fluid = fluid.copy();
        this.otherFluid = otherFluid.copy();
    }

    public String getFluidId() { return fluidId(fluid); }
    public String getOtherFluidId() { return fluidId(otherFluid); }
    public FluidStack getFluid() { return fluid.copy(); }
    public FluidStack getOtherFluid() { return otherFluid.copy(); }
}
