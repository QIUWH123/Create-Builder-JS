package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** World checks for native Create transport and material-preserving wrench conversions. */
@GameTestHolder("cbjs")
@PrefixGameTestTemplate(false)
public final class FluidPipeRuntimeGameTests {
    private static ScriptedFluidPipeBlock pipe(GameTestHelper helper) {
        Block block = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("kubejs:steel_fluid_pipe"));
        helper.assertTrue(block instanceof ScriptedFluidPipeBlock, "Missing supplied steel_fluid_pipe example");
        return (ScriptedFluidPipeBlock) block;
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_pipes")
    public static void wrenchKeepsMaterialWaterloggingAndDrops(GameTestHelper helper) {
        ScriptedFluidPipeBlock pipe = pipe(helper);
        BlockPos relative = new BlockPos(1, 1, 1);
        BlockPos pos = helper.absolutePos(relative);
        var level = helper.getLevel();
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        var context = new UseOnContext(level, null, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit);
        helper.setBlock(relative, pipe.getAxisState(Direction.Axis.X).setValue(BlockStateProperties.WATERLOGGED, true));
        helper.assertTrue(helper.getBlockEntity(relative) instanceof FluidPipeBlockEntity,
                "Regular custom pipe must create native fluid transport");
        helper.assertTrue(pipe.onWrenched(helper.getBlockState(relative), context).consumesAction(),
                "Wrench must create a glass window");
        BlockState glassState = helper.getBlockState(relative);
        helper.assertTrue(glassState.getBlock() == pipe.sourceBuilder().glassBlock()
                        && glassState.getValue(BlockStateProperties.AXIS) == Direction.Axis.X
                        && glassState.getValue(BlockStateProperties.WATERLOGGED),
                "Window conversion lost pipe family, axis, or waterlogging");
        helper.assertTrue(helper.getBlockEntity(relative) instanceof StraightPipeBlockEntity,
                "Glass pipe must create native visible-fluid transport");
        var glass = (ScriptedGlassFluidPipeBlock) glassState.getBlock();
        assertDrops(helper, glassState, pos, pipe);
        helper.assertTrue(glass.getCloneItemStack(level, pos, glassState).is(pipe.asItem()),
                "Glass pick block must return its material pipe");
        glass.onWrenched(glassState, context);
        BlockState restored = helper.getBlockState(relative);
        helper.assertTrue(restored.is(pipe) && restored.getValue(BlockStateProperties.WATERLOGGED),
                "Wrenching glass must restore its waterlogged material pipe");

        CustomEncasedPipeBlock copper = pipe.sourceBuilder().copperEncasedBlock();
        helper.assertTrue(copper != null, "Default copper casing companion is missing");
        copper.handleEncasing(restored, level, pos, ItemStack.EMPTY, null, InteractionHand.MAIN_HAND,
                hit);
        BlockState encasedState = helper.getBlockState(relative);
        helper.assertTrue(encasedState.is(copper), "Encasing selected the wrong material family");
        assertDrops(helper, encasedState, pos, pipe);
        helper.assertTrue(copper.getCloneItemStack(level, pos, encasedState).is(pipe.asItem()),
                "Encased pick block must return its material pipe");
        copper.onWrenched(encasedState, context);
        helper.assertTrue(helper.getBlockState(relative).is(pipe), "Unencasing substituted a copper pipe");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "cbjs_pipes", timeoutTicks = 240)
    public static void nativePumpTransfersThroughEveryPipeForm(GameTestHelper helper) {
        ScriptedFluidPipeBlock pipe = pipe(helper);
        BlockPos source = new BlockPos(0, 0, 1);
        BlockPos pumpPos = new BlockPos(1, 0, 1);
        BlockPos regular = new BlockPos(2, 0, 1);
        BlockPos glass = new BlockPos(2, 1, 1);
        BlockPos encased = new BlockPos(2, 2, 1);
        BlockPos target = new BlockPos(1, 2, 1);
        helper.setBlock(source, AllBlocks.FLUID_TANK.getDefaultState());
        helper.setBlock(pumpPos, AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST));
        helper.setBlock(regular, ports(pipe.defaultBlockState(), Direction.WEST, Direction.UP));
        helper.setBlock(glass, pipe.sourceBuilder().glassBlock().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
        helper.setBlock(encased, ports(pipe.sourceBuilder().copperEncasedBlock().defaultBlockState(),
                Direction.DOWN, Direction.WEST));
        helper.setBlock(target, AllBlocks.FLUID_TANK.getDefaultState());
        helper.runAtTickTime(2, () -> {
            var input = (FluidTankBlockEntity) helper.getBlockEntity(source);
            helper.assertTrue(input.getTankInventory().fill(new FluidStack(Fluids.WATER, 1000),
                    IFluidHandler.FluidAction.EXECUTE) == 1000, "Source tank failed to accept water");
            var pump = (PumpBlockEntity) helper.getBlockEntity(pumpPos);
            pump.updateSpeed = false;
            pump.setSpeed(64);
            pump.onSpeedChanged(0);
            FluidPropagator.propagateChangedPipe(helper.getLevel(), helper.absolutePos(regular), helper.getBlockState(regular));
        });
        // Supply pump speed directly so this compact fixture measures fluid transport without a motor network.
        helper.onEachTick(() -> {
            if (helper.getTick() < 2) return;
            var pump = (PumpBlockEntity) helper.getBlockEntity(pumpPos);
            pump.updateSpeed = false;
            float previous = pump.getSpeed();
            if (previous == 64) return;
            pump.setSpeed(64);
            pump.onSpeedChanged(previous);
        });
        helper.succeedWhen(() -> {
            var output = (FluidTankBlockEntity) helper.getBlockEntity(target);
            helper.assertTrue(output.getTankInventory().getFluidAmount() > 0
                            && output.getTankInventory().getFluid().getFluid() == Fluids.WATER,
                    "Native pump did not transfer water through regular, glass and encased custom pipes");
        });
    }

    private static BlockState ports(BlockState state, Direction... directions) {
        for (Direction side : Direction.values())
            state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), false);
        for (Direction side : directions) state = state.setValue(FluidPipeBlock.PROPERTY_BY_DIRECTION.get(side), true);
        return state;
    }

    private static void assertDrops(GameTestHelper helper, BlockState state, BlockPos pos, Block source) {
        var drops = Block.getDrops(state, helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos));
        helper.assertTrue(drops.size() == 1 && drops.get(0).is(source.asItem()) && drops.get(0).getCount() == 1,
                "Generated pipe form must drop exactly its regular material pipe");
    }

    private FluidPipeRuntimeGameTests() { }
}
