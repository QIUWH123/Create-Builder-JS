package net.quiwh.cbjs.kubejs;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.fluids.PipeConnectionState;

import java.util.Objects;

/** A connection started/stopped flowing, changed fluid, direction, or completion state. */
public final class FluidPipeFlowChangedEventJS extends FluidPipeEventJS {
    private final String side;
    private final PipeConnectionState previous;
    private final PipeConnectionState current;

    public FluidPipeFlowChangedEventJS(SmartBlockEntity entity, Direction side,
                                       PipeConnectionState previous, PipeConnectionState current) {
        super(entity);
        this.side = side.getName();
        this.previous = previous;
        this.current = current;
    }

    public String getSide() { return side; }
    public String getOldFluidId() { return fluidId(previous); }
    public String getFluidId() { return fluidId(current); }
    public FluidStack getOldFluid() { return previous.fluid(); }
    public FluidStack getFluid() { return current.fluid(); }
    public boolean getWasFlowing() { return previous.flowing(); }
    public boolean getIsFlowing() { return current.flowing(); }
    public boolean getWasInbound() { return previous.inbound(); }
    public boolean getIsInbound() { return current.inbound(); }
    public boolean getWasComplete() { return previous.complete(); }
    public boolean getIsComplete() { return current.complete(); }

    private static String fluidId(PipeConnectionState state) {
        return state.flowing() ? Objects.requireNonNull(ForgeRegistries.FLUIDS.getKey(state.fluidType())).toString()
                : "minecraft:empty";
    }
}
