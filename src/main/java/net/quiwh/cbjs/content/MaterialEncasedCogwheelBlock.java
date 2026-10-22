package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.quiwh.cbjs.kubejs.CompanionInheritance;

import java.util.function.Supplier;

/** Owns its unwrapped gear instead of using Create's hard-coded cogwheel entries. */
public class MaterialEncasedCogwheelBlock extends EncasedCogwheelBlock implements MaterialGear {
    private final GearModelSet models;
    private final Supplier<? extends MaterialCogwheelBlock> unwrapped;
    private final boolean shaftless;

    public MaterialEncasedCogwheelBlock(Properties properties, GearModelSet models, boolean large,
                                       Supplier<Block> casing, Supplier<? extends MaterialCogwheelBlock> unwrapped) {
        this(properties, models, large, casing, unwrapped, false);
    }

    public MaterialEncasedCogwheelBlock(Properties properties, GearModelSet models, boolean large,
                                       Supplier<Block> casing, Supplier<? extends MaterialCogwheelBlock> unwrapped,
                                       boolean shaftless) {
        super(properties, large, casing);
        this.models = models;
        this.unwrapped = unwrapped;
        this.shaftless = shaftless;
    }

    @Override
    public GearModelSet models() {
        return models;
    }

    @Override
    public boolean isEncased() {
        return true;
    }

    @Override
    public boolean isShaftless() {
        return shaftless;
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return !shaftless && super.hasShaftTowards(level, pos, state, face);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (shaftless && context.getClickedFace().getAxis() == state.getValue(AXIS))
            return InteractionResult.SUCCESS;
        return super.onWrenched(state, context);
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player,
                               InteractionHand hand, BlockHitResult hit) {
        BlockState encased = CompanionInheritance.transfer(state, defaultBlockState()).setValue(AXIS, state.getValue(AXIS))
                .setValue(TOP_SHAFT, false).setValue(BOTTOM_SHAFT, false);
        if (!shaftless) {
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() != state.getValue(AXIS)) continue;
                BlockPos adjacentPos = pos.relative(direction);
                BlockState adjacent = level.getBlockState(adjacentPos);
                if (adjacent.getBlock() instanceof com.simibubi.create.content.kinetics.base.IRotate rotating
                        && rotating.hasShaftTowards(level, adjacentPos, adjacent, direction.getOpposite()))
                    encased = encased.setValue(direction.getAxisDirection() == Direction.AxisDirection.POSITIVE
                            ? TOP_SHAFT : BOTTOM_SHAFT, true);
            }
        }
        KineticBlockEntity.switchToBlockState(level, pos, encased);
    }

    @Override
    public BlockEntityType<? extends SimpleKineticBlockEntity> getBlockEntityType() {
        return ModBlockEntities.ENCASED_COGWHEEL.get();
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide) {
            context.getLevel().levelEvent(2001, context.getClickedPos(), Block.getId(state));
            KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                    CompanionInheritance.transfer(state, unwrapped.get().defaultBlockState()).setValue(AXIS, state.getValue(AXIS)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level,
                                       BlockPos pos, Player player) {
        if (target instanceof BlockHitResult hit) {
            return new ItemStack(hit.getDirection().getAxis() == getRotationAxis(state)
                    ? getCasing() : unwrapped.get());
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        return ItemRequirement.of(unwrapped.get().defaultBlockState(), blockEntity);
    }
}
