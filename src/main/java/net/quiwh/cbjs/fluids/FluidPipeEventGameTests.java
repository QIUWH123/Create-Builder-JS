package net.quiwh.cbjs.fluids;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidReactions;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.quiwh.cbjs.kubejs.FluidPipeCollisionEventJS;
import net.quiwh.cbjs.kubejs.FluidPipeEventJS;
import net.quiwh.cbjs.kubejs.FluidPipeFlowEventJS;
import net.quiwh.cbjs.kubejs.FluidPipeFlowChangedEventJS;
import net.quiwh.cbjs.kubejs.FluidPipePressureChangedEventJS;
import net.quiwh.cbjs.kubejs.CBJSEvents;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises the transformed native Create tick/reaction in a real server world. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class FluidPipeEventGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final ResourceLocation PIPE_ID = ResourceLocation.parse("create:fluid_pipe");

    private static final class Observation {
        SmartBlockEntity entity;
        int flows;
        int pressures;
        int collisions;
        int nonmatching;
        int activeFlows;
        String mode = "pass";
        FluidPipeFlowChangedEventJS flow;
        FluidPipePressureChangedEventJS pressure;
        FluidPipeCollisionEventJS collision;
        FluidPipeFlowEventJS activeFlow;

        boolean matches(FluidPipeEventJS event) {
            return entity != null && event.getLevel() == entity.getLevel()
                    && event.getPos().equals(entity.getBlockPos());
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_fluid_events")
    public static void nativeActiveFlowRepeatsAndFiltersFluid(GameTestHelper helper) {
        var live = new AtomicReference<>(new Observation());
        CBJSEvents.FLUID_PIPE_FLOW.listenJava(ScriptType.SERVER,
                ResourceLocation.parse("minecraft:water"), raw -> {
                    var state = live.get();
                    var event = (FluidPipeFlowEventJS) raw;
                    if (state != null && state.matches(event)) {
                        state.activeFlows++;
                        state.activeFlow = event;
                    }
                    return null;
                });
        CBJSEvents.FLUID_PIPE_FLOW.listenJava(ScriptType.SERVER,
                ResourceLocation.parse("minecraft:lava"), raw -> {
                    var state = live.get();
                    if (state != null && state.matches((FluidPipeEventJS) raw)) state.nonmatching++;
                    return null;
                });
        try {
            Observation state = live.get();
            state.entity = place(helper);
            FluidTransportBehaviour transport = state.entity.getBehaviour(FluidTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Native pipe is missing its transport behaviour");
            transport.interfaces = new EnumMap<>(Direction.class);
            PipeConnection north = connection(state.entity.getBlockPos(), true, 64, 16);
            transport.interfaces.put(Direction.NORTH, north);
            waitingTick(transport);
            waitingTick(transport);
            helper.assertTrue(state.activeFlows == 2 && state.nonmatching == 0,
                    "Steady completed water flow must repeat on native ticks and skip lava listeners");
            helper.assertTrue(state.activeFlow.getBlockId().equals("create:fluid_pipe")
                            && state.activeFlow.getFluidId().equals("minecraft:water")
                            && state.activeFlow.getSide().equals("north") && state.activeFlow.getIsInbound(),
                    "Active flow payload lost pipe identity, fluid or connection direction");
            FluidPipeFlowEventJS first = state.activeFlow;
            transport.getFlow(Direction.NORTH).complete = false;
            waitingTick(transport);
            helper.assertTrue(state.activeFlows == 2, "An incomplete flow must not emit active flow");
            transport.getFlow(Direction.NORTH).complete = true;
            transport.getFlow(Direction.NORTH).fluid.setAmount(17);
            waitingTick(transport);
            helper.assertTrue(state.activeFlows == 3 && first.getFluid().getAmount() == 1000,
                    "Active flow must resume and retain defensive fluid snapshots");
            transport.interfaces.put(Direction.NORTH, connection(state.entity.getBlockPos(), false, 0, 0));
            waitingTick(transport);
            helper.assertTrue(state.activeFlows == 3, "Stopped flow must not emit active flow");
            helper.succeed();
        } finally {
            live.set(null);
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_fluid_events")
    public static void nativeTransportTickReportsChangesWithoutDuplicates(GameTestHelper helper) {
        var live = new AtomicReference<>(new Observation());
        // Listeners retain only this holder, cleared on both success and failure.
        CBJSEvents.FLUID_PIPE_FLOW_CHANGED.listenJava(ScriptType.SERVER, PIPE_ID, raw -> {
            var state = live.get();
            var event = (FluidPipeFlowChangedEventJS) raw;
            if (state != null && state.matches(event)) { state.flows++; state.flow = event; }
            return null;
        });
        CBJSEvents.FLUID_PIPE_PRESSURE_CHANGED.listenJava(ScriptType.SERVER, PIPE_ID, raw -> {
            var state = live.get();
            var event = (FluidPipePressureChangedEventJS) raw;
            if (state != null && state.matches(event)) { state.pressures++; state.pressure = event; }
            return null;
        });
        CBJSEvents.FLUID_PIPE_FLOW_CHANGED.listenJava(ScriptType.SERVER,
                ResourceLocation.parse("create:smart_fluid_pipe"), raw -> {
                    var state = live.get();
                    if (state != null && state.matches((FluidPipeEventJS) raw)) state.nonmatching++;
                    return null;
                });
        try {
            Observation state = live.get();
            state.entity = place(helper);
            FluidTransportBehaviour transport = state.entity.getBehaviour(FluidTransportBehaviour.TYPE);
            helper.assertTrue(transport != null, "Native pipe is missing its transport behaviour");
            transport.interfaces = new EnumMap<>(Direction.class);
            PipeConnection north = connection(state.entity.getBlockPos(), true, 64, 16);
            transport.interfaces.put(Direction.NORTH, north);
            waitingTick(transport);
            helper.assertTrue(state.flows == 1 && state.pressures == 1,
                    "Injected native transport tick must emit exactly one flow and pressure change");
            helper.assertTrue(state.flow.getBlockId().equals("create:fluid_pipe")
                            && state.flow.getSide().equals("north") && !state.flow.getWasFlowing()
                            && state.flow.getIsFlowing() && state.flow.getIsInbound() && state.flow.getIsComplete()
                            && state.flow.getOldFluidId().equals("minecraft:empty")
                            && state.flow.getFluidId().equals("minecraft:water"),
                    "Flow event lost native ID, direction, start state, or fluid snapshot");
            helper.assertTrue(state.pressure.getOldInboundPressure() == 0
                            && state.pressure.getInboundPressure() == 64
                            && state.pressure.getOldOutwardPressure() == 0
                            && state.pressure.getOutwardPressure() == 16,
                    "Pressure event lost old/current connection pressures");
            FluidPipeFlowChangedEventJS first = state.flow;
            waitingTick(transport);
            helper.assertTrue(state.flows == 1 && state.pressures == 1,
                    "An unchanged native tick duplicated events");

            transport.getFlow(Direction.NORTH).fluid.setAmount(23);
            waitingTick(transport);
            helper.assertTrue(state.flows == 1 && state.pressures == 1,
                    "Source stack amount changes must not be reported as transferred fluid");
            north.getPressure().set(true, 128F);
            waitingTick(transport);
            helper.assertTrue(state.flows == 1 && state.pressures == 2
                            && state.pressure.getOldInboundPressure() == 64
                            && state.pressure.getInboundPressure() == 128,
                    "Pressure-only change must retain its previous baseline");

            transport.interfaces.put(Direction.NORTH, connection(state.entity.getBlockPos(), false, 0, 0));
            waitingTick(transport);
            helper.assertTrue(state.flows == 2 && state.pressures == 3
                            && state.flow.getWasFlowing() && !state.flow.getIsFlowing()
                            && state.flow.getOldFluidId().equals("minecraft:water")
                            && state.flow.getFluidId().equals("minecraft:empty"),
                    "Removing a connection flow must emit its stop/empty transition");
            helper.assertTrue(first.getFluid().getAmount() == 1000 && first.getIsFlowing(),
                    "Later transport changes mutated an earlier event snapshot");
            helper.assertTrue(state.nonmatching == 0, "Exact block filter accepted the wrong native pipe ID");
            helper.succeed();
        } finally {
            live.set(null);
        }
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_fluid_events")
    public static void nativeCollisionCanCancelAndProtectReplacements(GameTestHelper helper) {
        var live = new AtomicReference<>(new Observation());
        CBJSEvents.FLUID_PIPE_COLLISION.listenJava(ScriptType.SERVER, PIPE_ID, raw -> {
            var state = live.get();
            var event = (FluidPipeCollisionEventJS) raw;
            if (state == null || !state.matches(event)) return null;
            state.collisions++;
            state.collision = event;
            if (state.mode.equals("cancel")) event.cancel();
            if (state.mode.equals("replace"))
                event.getLevel().setBlock(event.getPos(), Blocks.DIAMOND_BLOCK.defaultBlockState(), 3);
            return null;
        });
        try {
            Observation state = live.get();
            for (String mode : new String[]{"cancel", "replace", "pass"}) {
                state.entity = place(helper);
                state.mode = mode;
                state.collisions = 0;
                FluidReactions.handlePipeFlowCollision(helper.getLevel(), state.entity.getBlockPos(),
                        new FluidStack(Fluids.WATER, 1000), new FluidStack(Fluids.LAVA, 1000));
                helper.assertTrue(state.collisions == 1, mode + " must dispatch exactly one collision event");
                helper.assertTrue(state.collision.getFluidId().equals("minecraft:water")
                                && state.collision.getOtherFluidId().equals("minecraft:lava"),
                        "Collision payload lost either native fluid ID");
                helper.assertBlockPresent(mode.equals("cancel") ? AllBlocks.FLUID_PIPE.get()
                        : mode.equals("replace") ? Blocks.DIAMOND_BLOCK : Blocks.COBBLESTONE, POS);
            }
            helper.succeed();
        } finally {
            live.set(null);
        }
    }

    private static SmartBlockEntity place(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.AIR);
        helper.setBlock(POS, AllBlocks.FLUID_PIPE.get().getAxisState(Direction.Axis.Z));
        return (SmartBlockEntity) helper.getBlockEntity(POS);
    }

    private static PipeConnection connection(BlockPos pos, boolean flowing, float in, float out) {
        PipeConnection connection = new PipeConnection(Direction.NORTH);
        CompoundTag side = new CompoundTag();
        if (flowing) {
            CompoundTag flow = new FluidStack(Fluids.WATER, 1000).writeToNBT(new CompoundTag());
            flow.putBoolean("In", true);
            side.put("Flow", flow);
        }
        CompoundTag data = new CompoundTag();
        data.put("north", side);
        connection.deserializeNBT(data, pos, false);
        connection.getPressure().set(true, in);
        connection.getPressure().set(false, out);
        return connection;
    }

    private static void waitingTick(FluidTransportBehaviour transport) {
        // Native WAIT_FOR_PUMPS returns before flow simulation, so this checks the injected
        // observation of seeded native connection state without constructing a second network.
        transport.phase = FluidTransportBehaviour.UpdatePhase.WAIT_FOR_PUMPS;
        transport.tick();
    }

    private FluidPipeEventGameTests() { }
}
