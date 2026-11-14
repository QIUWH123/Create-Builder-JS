package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BrassTunnelItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Exercises actual NeoForge providers and native handlers with the registered startup families. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class TunnelCapabilityGameTests {
    @GameTest(template = "empty3x3x3", batch = "cbjs_tunnel_capabilities")
    public static void andesiteTunnelForwardsToNativeBelt(GameTestHelper helper) {
        assertAndesiteForwarding(helper, AllBlocks.BELT.get());
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_tunnel_capabilities")
    public static void andesiteTunnelForwardsToScriptedBelt(GameTestHelper helper) {
        assertAndesiteForwarding(helper, scriptedBlock(helper, ScriptedMechanicalBeltBlock.class));
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_tunnel_capabilities")
    public static void brassTunnelForwardsWithoutDistribution(GameTestHelper helper) {
        for (int row : new int[]{0, 2}) {
            var belt = setup(helper, row, row == 0 ? AllBlocks.BELT.get()
                    : scriptedBlock(helper, ScriptedMechanicalBeltBlock.class),
                    scriptedBlock(helper, ScriptedBrassTunnelBlock.class), BeltTunnelBlock.Shape.STRAIGHT);
            var tunnelPos = new BlockPos(1, 1, row);
            var tunnel = (BrassTunnelBlockEntity) helper.getBlockEntity(tunnelPos);
            helper.assertTrue(!tunnel.hasDistributionBehaviour(), "Straight isolated brass tunnel must forward to its belt");
            IItemHandler handler = tunnelHandler(helper, tunnelPos);
            helper.assertTrue(handler instanceof BrassTunnelItemHandler, "Brass tunnel must expose Create's distribution-aware handler");
            assertTransfer(helper, handler, belt);
            helper.assertTrue(tunnel.getStackToDistribute().isEmpty(), "Straight tunnel must not queue a distribution stack");
        }
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_tunnel_capabilities")
    public static void brassTunnelPreservesDistributionAndSimulation(GameTestHelper helper) {
        for (int row : new int[]{0, 2}) {
            var belt = setup(helper, row, row == 0 ? AllBlocks.BELT.get()
                    : scriptedBlock(helper, ScriptedMechanicalBeltBlock.class),
                    scriptedBlock(helper, ScriptedBrassTunnelBlock.class), BeltTunnelBlock.Shape.CROSS);
            var tunnelPos = new BlockPos(1, 1, row);
            var tunnel = (BrassTunnelBlockEntity) helper.getBlockEntity(tunnelPos);
            helper.assertTrue(tunnel.hasDistributionBehaviour(), "Cross tunnel must retain native distribution behavior");
            IItemHandler handler = tunnelHandler(helper, tunnelPos);
            helper.assertTrue(handler instanceof BrassTunnelItemHandler, "Brass tunnel must reuse its native handler");
            helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), true).isEmpty()
                            && tunnel.getStackToDistribute().isEmpty(),
                    "Simulated insertion must accept without queuing a distribution stack");
            helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                    "Distribution handler rejected an empty tunnel's input");
            helper.assertTrue(tunnel.getStackToDistribute().is(Items.IRON_INGOT)
                            && tunnel.getStackToDistribute().getCount() == 8
                            && belt.getInventory().getTransportedItems().isEmpty(),
                    "Distribution input must enter the native queue instead of bypassing it onto the belt");
            helper.assertTrue(handler.insertItem(0, new ItemStack(Items.GOLD_INGOT), false).getCount() == 1
                            && tunnel.getStackToDistribute().is(Items.IRON_INGOT),
                    "Occupied distribution queue must reject additional input");
        }
        helper.succeed();
    }

    private static void assertAndesiteForwarding(GameTestHelper helper, Block beltBlock) {
        var belt = setup(helper, 1, beltBlock, scriptedBlock(helper, ScriptedTunnelBlock.class),
                BeltTunnelBlock.Shape.STRAIGHT);
        var tunnelPos = new BlockPos(1, 1, 1);
        IItemHandler handler = tunnelHandler(helper, tunnelPos);
        IItemHandler beltHandler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(tunnelPos.below()), Direction.UP);
        helper.assertTrue(handler == beltHandler, "Andesite tunnel must lazily forward the exact handler of the belt below");
        for (Direction side : Direction.values())
            helper.assertTrue(helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(tunnelPos), side) == handler,
                    "Native tunnel capability must remain available from every side");
        assertTransfer(helper, handler, belt);
    }

    private static void assertTransfer(GameTestHelper helper, IItemHandler handler, BeltBlockEntity belt) {
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), true).isEmpty()
                        && belt.getInventory().getTransportedItems().isEmpty(),
                "Simulated tunnel insertion must leave the belt unchanged");
        helper.assertTrue(handler.insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(),
                "Tunnel failed to insert into the underlying belt");
        // Native belt insertion is queued until inventory tick, even while the belt is stationary.
        belt.getInventory().tick();
        var beltHandler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, belt.getBlockPos(), Direction.UP);
        helper.assertTrue(beltHandler != null, "Underlying belt must expose its native item handler");
        // Native brass getStackInSlot reports its distribution queue, even in forwarding mode.
        helper.assertTrue(handler.extractItem(0, 3, true).getCount() == 3
                        && beltHandler.getStackInSlot(0).getCount() == 8,
                "Simulated tunnel extraction changed the underlying belt");
        ItemStack extracted = handler.extractItem(0, 3, false);
        helper.assertTrue(extracted.is(Items.IRON_INGOT) && extracted.getCount() == 3
                        && beltHandler.getStackInSlot(0).getCount() == 5,
                "Tunnel failed to extract from the underlying belt");
    }

    private static BeltBlockEntity setup(GameTestHelper helper, int row, Block beltBlock,
            Block tunnelBlock, BeltTunnelBlock.Shape shape) {
        var beltState = beltBlock.defaultBlockState().setValue(BeltBlock.SLOPE, BeltSlope.HORIZONTAL)
                .setValue(BeltBlock.HORIZONTAL_FACING, Direction.EAST).setValue(BeltBlock.CASING, true);
        helper.setBlock(new BlockPos(0, 0, row), beltState.setValue(BeltBlock.PART, BeltPart.START));
        helper.setBlock(new BlockPos(1, 0, row), beltState.setValue(BeltBlock.PART, BeltPart.MIDDLE));
        helper.setBlock(new BlockPos(2, 0, row), beltState.setValue(BeltBlock.PART, BeltPart.END));
        BeltBlock.initBelt(helper.getLevel(), helper.absolutePos(new BlockPos(0, 0, row)));
        var belt = (BeltBlockEntity) helper.getBlockEntity(new BlockPos(1, 0, row));
        helper.assertTrue(belt.beltLength == 3, "Fixture belt must initialize before querying tunnel capabilities");
        var tunnelPos = new BlockPos(1, 1, row);
        helper.setBlock(tunnelPos, tunnelBlock.defaultBlockState()
                .setValue(BeltTunnelBlock.HORIZONTAL_AXIS, Direction.Axis.X).setValue(BeltTunnelBlock.SHAPE, shape));
        ((com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity) helper.getBlockEntity(tunnelPos))
                .updateTunnelConnections();
        return belt;
    }

    private static IItemHandler tunnelHandler(GameTestHelper helper, BlockPos pos) {
        var handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(handler != null, "Scripted tunnel is missing its registered NeoForge item capability");
        return handler;
    }

    private static Block scriptedBlock(GameTestHelper helper, Class<? extends Block> type) {
        for (Block block : BuiltInRegistries.BLOCK) if (type.isInstance(block)) return block;
        helper.fail("Missing startup fixture for " + type.getSimpleName());
        throw new AssertionError("unreachable");
    }

    private TunnelCapabilityGameTests() { }
}
