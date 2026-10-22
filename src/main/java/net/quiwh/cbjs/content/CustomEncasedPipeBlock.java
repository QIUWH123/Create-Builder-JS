package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.AxisPipeBlock;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.ticks.TickPriority;
import net.quiwh.cbjs.kubejs.CompanionInheritance;

import java.util.function.Supplier;

/** Keeps Create's pipe transport while accepting a script-defined casing. */
public class CustomEncasedPipeBlock extends EncasedPipeBlock {
    private final Supplier<FluidPipeBlock> original;

    public CustomEncasedPipeBlock(Properties properties, Supplier<Block> casing) {
        this(properties, casing, AllBlocks.FLUID_PIPE::get);
    }

    public CustomEncasedPipeBlock(Properties properties, Supplier<Block> casing, Supplier<FluidPipeBlock> original) {
        super(properties, casing);
        this.original = original;
    }

    public FluidPipeBlock originalPipe() { return original.get(); }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return CustomCasingBlockEntities.ENCASED_FLUID_PIPE.get();
    }

    @Override public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter level,
                                                BlockPos pos, Player player) {
        return new ItemStack(originalPipe());
    }

    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(originalPipe());
    }

    @Override public ItemRequirement getRequiredItems(BlockState state, BlockEntity entity) {
        return ItemRequirement.of(originalPipe().defaultBlockState(), entity);
    }

    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        level.levelEvent(2001, pos, Block.getId(state));
        FluidPipeBlock pipe = originalPipe();
        BlockState regular = CompanionInheritance.transfer(state, transferSixWayProperties(state, pipe.defaultBlockState()));
        Direction first = Direction.UP;
        for (Direction direction : Direction.values()) {
            if (!state.getValue(FACING_TO_PROPERTY_MAP.get(direction))) continue;
            first = direction;
            break;
        }
        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, pipe.updateBlockState(regular, first, null, level, pos));
        FluidTransportBehaviour.loadFlows(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem,
            Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, CompanionInheritance.transfer(state, transferSixWayProperties(state, defaultBlockState())));
        FluidTransportBehaviour.loadFlows(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block changedBlock,
                                BlockPos neighborPos, boolean isMoving) {
        DebugPackets.sendNeighborsUpdatePacket(level, pos);
        if (level.isClientSide) {
            return;
        }

        // Create's validator only recognizes its exact encased-pipe registry entry.
        // Keep its neighbor filtering while treating this subclass as an encased pipe.
        Block neighbor = level.getBlockState(neighborPos).getBlock();
        if (neighbor instanceof FluidPipeBlock || neighbor instanceof AxisPipeBlock
                || neighbor instanceof PumpBlock || neighbor instanceof LiquidBlock) {
            return;
        }
        for (Direction direction : Direction.values()) {
            if (pos.relative(direction).equals(neighborPos)
                    && state.getValue(FACING_TO_PROPERTY_MAP.get(direction))) {
                level.scheduleTick(pos, this, 1, TickPriority.HIGH);
                return;
            }
        }
    }
}
