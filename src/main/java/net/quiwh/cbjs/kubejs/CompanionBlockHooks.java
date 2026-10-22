package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.*;
import dev.latvian.mods.kubejs.block.callbacks.*;
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
    private final BlockBuilder builder;
    public CompanionBlockHooks(BlockBuilder builder) { this.builder = builder; }

    public static void properties(BlockBehaviour.Properties properties, StateDefinition.Builder<Block, BlockState> definition,
                                  Set<String> reserved) {
        if (properties instanceof KubeJSBlockProperties scripted)
            for (Property<?> property : scripted.blockBuilder.blockStateProperties)
                if (!reserved.contains(property.getName())) definition.add(property);
    }
    public BlockState defaultState(BlockState state) {
        if (builder.defaultStateModification == null) return state;
        var callback = new BlockStateModifyCallbackJS(state);
        return safe(builder.defaultStateModification, callback) ? callback.getState() : state;
    }
    public BlockState placement(BlockPlaceContext context, BlockState state) {
        if (state == null || builder.placementStateModification == null) return state;
        var callback = new BlockStateModifyPlacementCallbackJS(context, state.getBlock());
        for (var property : state.getProperties()) copy(callback, state, property);
        return safe(builder.placementStateModification, callback) ? callback.getState() : state;
    }
    private static <T extends Comparable<T>> void copy(BlockStateModifyCallbackJS callback, BlockState state, Property<T> property) {
        callback.setValue(property, state.getValue(property));
    }
    public ItemStack pick(BlockGetter level, BlockPos pos, BlockState state, Supplier<ItemStack> fallback) {
        if (builder.pickBlockCallback != null) {
            var callback = new PickBlockCallbackJS(level, pos, state);
            if (safe(builder.pickBlockCallback, callback) && callback.item != null) return new ItemStack(callback.item);
        }
        return fallback.get();
    }
    public boolean replace(BlockState state, BlockPlaceContext context, BooleanSupplier fallback) {
        return builder.canBeReplacedFunction == null ? fallback.getAsBoolean()
                : builder.canBeReplacedFunction.test(new CanBeReplacedCallbackJS(context, state));
    }
    public void step(Level level, BlockPos pos, BlockState state, Entity entity, Runnable fallback) {
        if (builder.stepOnCallback == null) fallback.run();
        else safe(builder.stepOnCallback, new EntitySteppedOnBlockCallbackJS(level, entity, pos, state));
    }
    public void fall(Level level, BlockPos pos, BlockState state, Entity entity, float distance, Runnable fallback) {
        if (builder.fallOnCallback == null) fallback.run();
        else safe(builder.fallOnCallback, new EntityFallenOnBlockCallbackJS(level, entity, pos, state, distance));
    }
    public void bounce(BlockGetter level, Entity entity, Runnable fallback) {
        if (builder.afterFallenOnCallback == null) { fallback.run(); return; }
        var callback = new AfterEntityFallenOnBlockCallbackJS(level, entity);
        safe(builder.afterFallenOnCallback, callback);
        if (!callback.hasChangedVelocity()) fallback.run();
    }
    public void explode(Level level, BlockPos pos, Explosion explosion, Runnable fallback) {
        if (builder.explodedCallback == null) fallback.run();
        else safe(builder.explodedCallback, new BlockExplodedCallbackJS(level, pos, explosion));
    }
    public BlockState rotate(BlockState state, Rotation rotation, Supplier<BlockState> fallback) {
        if (builder.rotateStateModification == null) return fallback.get();
        var callback = new BlockStateRotateCallbackJS(state, rotation);
        return safe(builder.rotateStateModification, callback) ? callback.getState() : fallback.get();
    }
    public BlockState mirror(BlockState state, Mirror mirror, Supplier<BlockState> fallback) {
        if (builder.mirrorStateModification == null) return fallback.get();
        var callback = new BlockStateMirrorCallbackJS(state, mirror);
        return safe(builder.mirrorStateModification, callback) ? callback.getState() : fallback.get();
    }
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit, Supplier<InteractionResult> fallback) {
        if (builder.rightClick == null) return fallback.get();
        if (!level.isClientSide) safe(builder.rightClick, new BlockRightClickedEventJS(player, hand, pos, hit.getDirection()));
        return InteractionResult.SUCCESS;
    }
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, Runnable fallback) {
        if (builder.randomTickCallback == null) fallback.run();
        else safe(builder.randomTickCallback, new RandomTickCallbackJS(new dev.latvian.mods.kubejs.level.BlockContainerJS(level, pos), random));
    }
    private static <T> boolean safe(Consumer<T> consumer, T callback) {
        try { consumer.accept(callback); return true; }
        catch (Exception exception) {
            dev.latvian.mods.kubejs.util.ConsoleJS.SERVER.error("Companion block callback", exception); return false;
        }
    }
}
