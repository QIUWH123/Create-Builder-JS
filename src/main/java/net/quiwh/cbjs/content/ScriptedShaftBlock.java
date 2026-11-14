package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.quiwh.cbjs.kubejs.ShaftBuilder;

import dev.latvian.mods.kubejs.block.*;
import dev.latvian.mods.kubejs.block.callback.*;
import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Forwards ordinary script block hooks while retaining Create's native shaft mechanics. */
public final class ScriptedShaftBlock extends ShaftBlock implements BlockKJS {
    private static final int MATERIAL_PLACEMENT_HELPER = net.createmod.catnip.placement.PlacementHelpers.register(
            new com.simibubi.create.foundation.placement.PoleHelper<net.minecraft.core.Direction.Axis>(
                    state -> state.getBlock() instanceof com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock,
                    state -> state.getValue(AXIS), AXIS) {
                @Override public java.util.function.Predicate<net.minecraft.world.item.ItemStack> getItemPredicate() {
                    return stack -> stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                            && item.getBlock() instanceof com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
                }
                @Override public java.util.function.Predicate<BlockState> getStatePredicate() {
                    return state -> state.getBlock() instanceof ScriptedShaftBlock;
                }
            });
    private final ShaftBuilder builder;
    private final VoxelShape customShape;
    public ScriptedShaftBlock(ShaftBuilder builder) {
        super(builder.createProperties());
        this.builder = builder;
        // Match BasicBlockJS: startup shapes are fixed when the block is created.
        customShape = builder.customShape == null || builder.customShape.isEmpty()
                ? null : BlockBuilder.createShape(builder.customShape);
        if (builder.defaultStateModification != null) {
            var callback = new BlockStateModifyCallback(defaultBlockState());
            if (safeCallback(builder.defaultStateModification, callback, "Shaft default state callback"))
                registerDefaultState(callback.getState());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyProperty(BlockStateModifyCallback callback, BlockState state, Property property) {
        callback.setValue(property, state.getValue(property));
    }
    private static <T> boolean safeCallback(java.util.function.Consumer<T> consumer, T callback, String message) {
        try { consumer.accept(callback); return true; }
        catch (Exception exception) { dev.latvian.mods.kubejs.script.ConsoleJS.SERVER.error(message, exception); return false; }
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, LevelReader level, BlockPos pos, Player player) {
        if (builder.pickBlockCallback != null) {
            var callback = new net.quiwh.cbjs.kubejs.PickBlockCallback(level, pos, state);
            if (safeCallback(builder.pickBlockCallback, callback, "Shaft pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return super.getCloneItemStack(state, hit, level, pos, player);
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        if (builder.pickBlockCallback != null) {
            var callback = new net.quiwh.cbjs.kubejs.PickBlockCallback(level, pos, state);
            if (safeCallback(builder.pickBlockCallback, callback, "Shaft pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return super.getCloneItemStack(level, pos, state);
    }
    @Override public BlockBuilder kjs$getBlockBuilder() { return builder; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        if (properties instanceof KubeJSBlockProperties scripted)
            for (Property<?> property : scripted.blockBuilder.blockStateProperties)
                if (!java.util.Set.of("axis", "waterlogged").contains(property.getName()))
                    definition.add(property);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return customShape == null ? super.getShape(state, level, pos, context) : customShape;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && builder.placementStateModification != null) {
            var callback = new BlockStateModifyPlacementCallback(context, this);
            for (var property : state.getProperties()) copyProperty(callback, state, property);
            safeCallback(builder.placementStateModification, callback, "Shaft placement callback");
            return ShaftBlock.pickCorrectShaftType(callback.getState(), context.getLevel(), context.getClickedPos());
        }
        return state;
    }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return builder.canBeReplacedFunction == null ? super.canBeReplaced(state, context)
                : builder.canBeReplacedFunction.test(new CanBeReplacedCallback(context, state));
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (builder.stepOnCallback == null) super.stepOn(level, pos, state, entity);
        else safeCallback(builder.stepOnCallback, new EntityBlockCallback(level, entity, pos, state), "Shaft step callback");
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (builder.fallOnCallback == null) super.fallOn(level, state, pos, entity, distance);
        else safeCallback(builder.fallOnCallback, new EntityFallenOnBlockCallback(level, entity, pos, state, distance), "Shaft fall callback");
    }
    @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (builder.afterFallenOnCallback == null) { super.updateEntityAfterFallOn(level, entity); return; }
        var callback = new AfterEntityFallenOnBlockCallback(level, entity);
        safeCallback(builder.afterFallenOnCallback, callback, "Shaft bounce callback");
        if (!callback.hasChangedVelocity()) super.updateEntityAfterFallOn(level, entity);
    }
    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (builder.explodedCallback == null) super.wasExploded(level, pos, explosion);
        else safeCallback(builder.explodedCallback, new BlockExplodedCallback(level, pos, explosion), "Shaft explosion callback");
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        if (builder.rotateStateModification == null) return super.rotate(state, rotation);
        var callback = new BlockStateRotateCallback(state, rotation);
        return safeCallback(builder.rotateStateModification, callback, "Shaft rotation callback") ? callback.getState() : super.rotate(state, rotation);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (builder.mirrorStateModification == null) return super.mirror(state, mirror);
        var callback = new BlockStateMirrorCallback(state, mirror);
        return safeCallback(builder.mirrorStateModification, callback, "Shaft mirror callback") ? callback.getState() : super.mirror(state, mirror);
    }
    @Override public ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (builder.rightClick == null) {
            ItemInteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hit);
            if (result.consumesAction() || player.isShiftKeyDown() || !player.mayBuild()) return result;
            var helper = net.createmod.catnip.placement.PlacementHelpers.get(MATERIAL_PLACEMENT_HELPER);
            var held = player.getItemInHand(hand);
            if (helper.matchesItem(held)) return helper.getOffset(player, level, state, pos, hit)
                    .placeInWorld(level, (net.minecraft.world.item.BlockItem) held.getItem(), player, hand, hit);
            return result;
        }
        if (!level.isClientSide) builder.rightClick.accept(new BlockRightClickedKubeEvent(player.getItemInHand(hand), player, hand, pos, hit.getDirection(), hit));
        return ItemInteractionResult.SUCCESS;
    }
    @Override public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (builder.rightClick == null) return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide) builder.rightClick.accept(new BlockRightClickedKubeEvent(
                player.getMainHandItem(), player, InteractionHand.MAIN_HAND, pos, hit.getDirection(), hit));
        return InteractionResult.SUCCESS;
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (builder.randomTickCallback != null)
            safeCallback(builder.randomTickCallback, new RandomTickCallback(new dev.latvian.mods.kubejs.level.CachedLevelBlock(level, pos), random), "Shaft random tick callback");
        else super.randomTick(state, level, pos, random);
    }
    @Override public boolean isRandomlyTicking(BlockState state) { return builder.randomTickCallback != null || super.isRandomlyTicking(state); }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return builder.transparent || super.propagatesSkylightDown(state, level, pos);
    }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return builder.transparent ? Shapes.empty() : super.getVisualShape(state, level, pos, context);
    }
    @Override public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        return builder.transparent && adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return builder.transparent ? 1 : super.getShadeBrightness(state, level, pos);
    }

    public ShaftBuilder sourceBuilder() { return builder; }

    @Override public BlockEntityType<? extends BracketedKineticBlockEntity> getBlockEntityType() {
        return ScriptedShaftBlockEntities.SHAFT.get();
    }
}
