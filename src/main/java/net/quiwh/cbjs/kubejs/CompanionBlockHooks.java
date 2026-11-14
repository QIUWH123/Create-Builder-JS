package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.*;
import dev.latvian.mods.kubejs.block.callback.*;
import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.Set;
import java.util.function.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;

/** Shared ordinary KubeJS hooks for native casing/glass shapes and mechanics. */
@HideFromJS
public final class CompanionBlockHooks {
    private final CBJSBlockBuilder builder;
    public CompanionBlockHooks(CBJSBlockBuilder builder) { this.builder = builder; }

    public static void properties(BlockBehaviour.Properties properties, StateDefinition.Builder<Block, BlockState> definition,
                                  Set<String> reserved) {
        if (properties instanceof KubeJSBlockProperties scripted)
            for (Property<?> property : scripted.blockBuilder.blockStateProperties)
                if (!reserved.contains(property.getName())) definition.add(property);
    }
    public BlockState defaultState(BlockState state) {
        if (builder.defaultStateModification == null) return state;
        var callback = new BlockStateModifyCallback(state);
        return safe(builder.defaultStateModification, callback) ? callback.getState() : state;
    }
    public BlockState placement(BlockPlaceContext context, BlockState state) {
        if (state == null || builder.placementStateModification == null) return state;
        var callback = new BlockStateModifyPlacementCallback(context, state.getBlock());
        for (var property : state.getProperties()) copy(callback, state, property);
        return safe(builder.placementStateModification, callback) ? callback.getState() : state;
    }
    private static <T extends Comparable<T>> void copy(BlockStateModifyCallback callback, BlockState state, Property<T> property) {
        callback.setValue(property, state.getValue(property));
    }
    public ItemStack pick(BlockGetter level, BlockPos pos, BlockState state, Supplier<ItemStack> fallback) {
        if (builder.pickBlockCallback != null) {
            var callback = new PickBlockCallback(level, pos, state);
            if (safe(builder.pickBlockCallback, callback) && callback.item != null) return new ItemStack(callback.item);
        }
        return fallback.get();
    }
    public boolean replace(BlockState state, BlockPlaceContext context, BooleanSupplier fallback) {
        return builder.canBeReplacedFunction == null ? fallback.getAsBoolean()
                : builder.canBeReplacedFunction.test(new CanBeReplacedCallback(context, state));
    }
    public void step(Level level, BlockPos pos, BlockState state, Entity entity, Runnable fallback) {
        if (builder.stepOnCallback == null) fallback.run();
        else safe(builder.stepOnCallback, new EntityBlockCallback(level, entity, pos, state));
    }
    public void fall(Level level, BlockPos pos, BlockState state, Entity entity, float distance, Runnable fallback) {
        if (builder.fallOnCallback == null) fallback.run();
        else safe(builder.fallOnCallback, new EntityFallenOnBlockCallback(level, entity, pos, state, distance));
    }
    public void bounce(BlockGetter level, Entity entity, Runnable fallback) {
        if (builder.afterFallenOnCallback == null) { fallback.run(); return; }
        var callback = new AfterEntityFallenOnBlockCallback(level, entity);
        safe(builder.afterFallenOnCallback, callback);
        if (!callback.hasChangedVelocity()) fallback.run();
    }
    public void explode(Level level, BlockPos pos, Explosion explosion, Runnable fallback) {
        if (builder.explodedCallback == null) fallback.run();
        else safe(builder.explodedCallback, new BlockExplodedCallback(level, pos, explosion));
    }
    public BlockState rotate(BlockState state, Rotation rotation, Supplier<BlockState> fallback) {
        if (builder.rotateStateModification == null) return fallback.get();
        var callback = new BlockStateRotateCallback(state, rotation);
        return safe(builder.rotateStateModification, callback) ? callback.getState() : fallback.get();
    }
    public BlockState mirror(BlockState state, Mirror mirror, Supplier<BlockState> fallback) {
        if (builder.mirrorStateModification == null) return fallback.get();
        var callback = new BlockStateMirrorCallback(state, mirror);
        return safe(builder.mirrorStateModification, callback) ? callback.getState() : fallback.get();
    }
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit,
                                           Supplier<ItemInteractionResult> fallback) {
        if (builder.rightClick == null) return fallback.get();
        if (!level.isClientSide) safe(builder.rightClick,
                new BlockRightClickedKubeEvent(stack, player, hand, pos, hit.getDirection(), hit));
        return ItemInteractionResult.SUCCESS;
    }
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                             BlockHitResult hit, Supplier<InteractionResult> fallback) {
        if (builder.rightClick == null) return fallback.get();
        if (!level.isClientSide) safe(builder.rightClick,
                new BlockRightClickedKubeEvent(ItemStack.EMPTY, player, InteractionHand.MAIN_HAND, pos, hit.getDirection(), hit));
        return InteractionResult.SUCCESS;
    }
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Runnable fallback) {
        if (builder.randomTickCallback == null) fallback.run();
        else safe(builder.randomTickCallback, new RandomTickCallback(new dev.latvian.mods.kubejs.level.CachedLevelBlock(level, pos), random));
    }
    private static <T> boolean safe(Consumer<T> consumer, T callback) {
        try { consumer.accept(callback); return true; }
        catch (Exception exception) {
            dev.latvian.mods.kubejs.script.ConsoleJS.SERVER.error("Companion block callback", exception); return false;
        }
    }
}
