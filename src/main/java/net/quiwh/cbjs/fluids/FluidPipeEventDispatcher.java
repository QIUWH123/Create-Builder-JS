package net.quiwh.cbjs.fluids;

import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.IAxisPipe;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.FluidPipeFlowChangedEventJS;
import net.quiwh.cbjs.kubejs.FluidPipeFlowEventJS;
import net.quiwh.cbjs.kubejs.FluidPipeFlowFilters;
import net.quiwh.cbjs.kubejs.FluidPipePressureChangedEventJS;
import net.quiwh.cbjs.kubejs.CBJSEvents;

/** Observes Create's existing six local connections; never traverses the fluid network. */
public final class FluidPipeEventDispatcher {
    private static final Direction[] SIDES = Direction.values();

    public static boolean isPipe(Block block) {
        return block instanceof FluidPipeBlock || block instanceof EncasedPipeBlock || block instanceof IAxisPipe;
    }

    public static boolean isCurrent(SmartBlockEntity entity) {
        var level = entity.getLevel();
        return level != null && !level.isClientSide && !entity.isRemoved() && !entity.isVirtual()
                && level.getBlockEntity(entity.getBlockPos()) == entity;
    }

    /** Active-flow selectors are checked before snapshot allocation; only changes need baselines. */
    public static PipeStateTracker observe(FluidTransportBehaviour transport, PipeStateTracker tracker) {
        if (!CBJSEvents.FLUID_PIPE_FLOW_CHANGED.hasListeners()
                && !CBJSEvents.FLUID_PIPE_PRESSURE_CHANGED.hasListeners()
                && !CBJSEvents.FLUID_PIPE_FLOW.hasListeners()) return null;
        SmartBlockEntity entity = transport.blockEntity;
        var level = entity.getLevel();
        if (level == null || level.isClientSide || !isPipe(entity.getBlockState().getBlock())) return null;
        var block = entity.getBlockState().getBlock();
        var id = ForgeRegistries.BLOCKS.getKey(block);
        boolean flowListener = CBJSEvents.FLUID_PIPE_FLOW_CHANGED.hasListeners(id);
        boolean pressureListener = CBJSEvents.FLUID_PIPE_PRESSURE_CHANGED.hasListeners(id);
        boolean activeListener = FluidPipeFlowFilters.hasListeners(block);
        boolean trackChanges = flowListener || pressureListener;
        if (!trackChanges && !activeListener) return null;
        if (!isCurrent(entity)) return null;
        if (!trackChanges) tracker = null;
        else if (tracker == null) tracker = new PipeStateTracker();

        for (Direction side : SIDES) {
            // Read existing interfaces directly; getConnection would initialize transport data.
            var connection = transport.interfaces == null ? null : transport.interfaces.get(side);
            var flow = connection == null ? null : transport.getFlow(side);
            boolean notifyActive = activeListener && flow != null && flow.complete && !flow.fluid.isEmpty()
                    && FluidPipeFlowFilters.hasListeners(block, flow.fluid.getFluid());
            var activeFluidId = notifyActive ? ForgeRegistries.FLUIDS.getKey(flow.fluid.getFluid()) : null;
            // Sample before any callbacks; they can mutate the live connection in place.
            var activeState = notifyActive ? new PipeConnectionState(true, flow.fluid,
                    flow.inbound, true, 0, 0) : null;
            if (trackChanges) {
                var pressure = connection == null ? null : connection.getPressure();
                var previous = tracker.observe(side, flow != null, flow == null ? FluidStack.EMPTY : flow.fluid,
                        flow != null && flow.inbound, flow != null && flow.complete,
                        pressure == null ? 0 : pressure.getFirst(), pressure == null ? 0 : pressure.getSecond());
                var current = tracker.current(side);
                // Advance the baseline before dispatch. Scripts can mutate or replace the pipe.
                if (previous != current) {
                    if (flowListener && current.flowChangedFrom(previous)) {
                        CBJSEvents.FLUID_PIPE_FLOW_CHANGED.post(
                                new FluidPipeFlowChangedEventJS(entity, side, previous, current), id);
                        if (!isCurrent(entity)) return null;
                    }
                    if (pressureListener && current.pressureChangedFrom(previous)) {
                        CBJSEvents.FLUID_PIPE_PRESSURE_CHANGED.post(
                                new FluidPipePressureChangedEventJS(entity, side, previous, current), id);
                        if (!isCurrent(entity)) return null;
                    }
                }
            }
            // Completed flow is Create's connection state, not a measurement of tank transfer.
            // With no change listeners, the fluid filter is checked before copying a snapshot.
            if (notifyActive) {
                CBJSEvents.FLUID_PIPE_FLOW.post(new FluidPipeFlowEventJS(entity, side, activeState), activeFluidId);
                if (!isCurrent(entity)) return null;
            }
        }
        return tracker;
    }

    private FluidPipeEventDispatcher() { }
}
