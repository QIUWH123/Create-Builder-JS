package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** In-world native filter and transfer checks for the supplied custom fluid devices. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class SmartFluidPipeRuntimeGameTests {
    private static ScriptedSmartFluidPipeBlock smartPipe(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("kubejs:steel_smart_pipe"));
        helper.assertTrue(block instanceof ScriptedSmartFluidPipeBlock,
                "Missing supplied steel_smart_pipe example");
        return (ScriptedSmartFluidPipeBlock) block;
    }

    private static ScriptedPumpBlock pump(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("kubejs:steel_mechanical_pump"));
        helper.assertTrue(block instanceof ScriptedPumpBlock, "Missing supplied steel_mechanical_pump example");
        return (ScriptedPumpBlock) block;
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_fluid_devices")
    public static void smartPipeKeepsNativeFilterAndWaterlogging(GameTestHelper helper) {
        ScriptedSmartFluidPipeBlock pipe = smartPipe(helper);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, pipe.defaultBlockState().setValue(SmartFluidPipeBlock.FACE, AttachFace.WALL)
                .setValue(BlockStateProperties.WATERLOGGED, true));
        helper.runAtTickTime(2, () -> {
            var entity = helper.getBlockEntity(pos);
            helper.assertTrue(entity instanceof SmartFluidPipeBlockEntity,
                    "Custom smart pipe must create native filtering fluid transport");
            helper.assertTrue(entity.getType().isValid(helper.getBlockState(pos)),
                    "Custom native block entity type must accept its scripted block");
            var smart = (SmartFluidPipeBlockEntity) entity;
            FilteringBehaviour filter = smart.getBehaviour(FilteringBehaviour.TYPE);
            helper.assertTrue(filter != null && smart.getBehaviour(FluidTransportBehaviour.TYPE) != null,
                    "Custom smart pipe must install both native filter controls and transport");
            helper.assertTrue(filter.setFilter(new ItemStack(Items.WATER_BUCKET)),
                    "Native fluid filter must accept a water bucket");
            helper.assertTrue(filter.test(new FluidStack(Fluids.WATER, 1000))
                            && !filter.test(new FluidStack(Fluids.LAVA, 1000)),
                    "Native filter must distinguish water and lava");
            BlockState state = helper.getBlockState(pos);
            helper.assertTrue(pipe.getAxis(state) == Direction.Axis.Y
                            && state.getValue(BlockStateProperties.WATERLOGGED),
                    "Custom smart pipe lost native vertical ports or waterlogging");
            BlockState rotated = pipe.getRotatedBlockState(state, Direction.UP);
            helper.assertTrue(rotated.getBlock() == pipe && rotated.getValue(BlockStateProperties.WATERLOGGED),
                    "Native wrench rotation must preserve the scripted smart pipe and waterlogging");
            helper.succeed();
        });
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_fluid_devices", timeoutTicks = 300)
    public static void customPumpTransfersOnlyTheSmartPipesFilteredFluid(GameTestHelper helper) {
        ScriptedSmartFluidPipeBlock smart = smartPipe(helper);
        ScriptedPumpBlock pump = pump(helper);
        BlockPos source = new BlockPos(0, 0, 1);
        BlockPos pumpPos = new BlockPos(1, 0, 1);
        BlockPos lowerCorner = new BlockPos(2, 0, 1);
        BlockPos smartPos = new BlockPos(2, 1, 1);
        BlockPos upperCorner = new BlockPos(2, 2, 1);
        BlockPos target = new BlockPos(1, 2, 1);
        BlockState upperState = ports(AllBlocks.FLUID_PIPE.getDefaultState(), Direction.DOWN, Direction.WEST);
        helper.setBlock(source, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(pumpPos, pump.defaultBlockState().setValue(PumpBlock.FACING, Direction.EAST));
        helper.setBlock(lowerCorner, ports(AllBlocks.FLUID_PIPE.getDefaultState(), Direction.WEST, Direction.UP));
        helper.setBlock(smartPos, smart.defaultBlockState().setValue(SmartFluidPipeBlock.FACE, AttachFace.WALL));
        helper.setBlock(upperCorner, upperState);
        helper.setBlock(target, AllBlocks.FLUID_TANK.getDefaultState());
        helper.runAtTickTime(2, () -> {
            var input = (FluidTankBlockEntity) helper.getBlockEntity(source);
            var filter = ((SmartFluidPipeBlockEntity) helper.getBlockEntity(smartPos)).getBehaviour(FilteringBehaviour.TYPE);
            helper.assertTrue(filter.setFilter(new ItemStack(Items.LAVA_BUCKET)), "Lava filter was not accepted");
            helper.assertTrue(input.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000),
                    IFluidHandler.FluidAction.EXECUTE) == 1000, "Source tank failed to accept water");
            keepPumpRunning(helper, pumpPos);
            FluidPropagator.propagateChangedPipe(helper.getLevel(), helper.absolutePos(lowerCorner),
                    helper.getBlockState(lowerCorner));
        });
        // Direct speed isolates fluid transport from the separate kinetic-network tests.
        helper.onEachTick(() -> { if (helper.getTick() >= 2) keepPumpRunning(helper, pumpPos); });
        helper.runAtTickTime(60, () -> {
            var output = (FluidTankBlockEntity) helper.getBlockEntity(target);
            helper.assertTrue(output.getTankInventory().getFluidAmount() == 0,
                    "Custom smart pipe passed water while filtering for lava");
            var filter = ((SmartFluidPipeBlockEntity) helper.getBlockEntity(smartPos)).getBehaviour(FilteringBehaviour.TYPE);
            helper.assertTrue(filter.setFilter(new ItemStack(Items.WATER_BUCKET)), "Water filter was not accepted");
            // Reconnecting a pipe must discover the custom pump when pressure propagation runs again.
            helper.setBlock(upperCorner, Blocks.AIR);
            helper.setBlock(upperCorner, upperState);
            FluidPropagator.propagateChangedPipe(helper.getLevel(), helper.absolutePos(upperCorner), upperState);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getTick() > 60, "Wait for the matching filter");
            var output = (FluidTankBlockEntity) helper.getBlockEntity(target);
            helper.assertTrue(output.getTankInventory().getFluidAmount() > 0
                            && output.getTankInventory().getFluid().getFluid() == Fluids.WATER,
                    "Custom pump did not transfer water through the matching scripted smart pipe filter");
        });
    }

    private static void keepPumpRunning(GameTestHelper helper, BlockPos pos) {
        var pump = (PumpBlockEntity) helper.getBlockEntity(pos);
        pump.updateSpeed = false;
        float previous = pump.getSpeed();
        if (previous == 64) return;
        pump.setSpeed(64);
        pump.onSpeedChanged(previous);
    }

    private static BlockState ports(BlockState state, Direction... directions) {
        for (Direction side : Direction.values())
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
        for (Direction side : directions)
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
        return state;
    }

    private SmartFluidPipeRuntimeGameTests() { }
}
