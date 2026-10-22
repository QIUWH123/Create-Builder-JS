package net.quiwh.cbjs.content;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.quiwh.cbjs.kubejs.CompanionBlockHooks;
import java.util.Set;
import java.util.function.Supplier;
import net.quiwh.cbjs.kubejs.ShaftBuilder;

/** Script callbacks on an encased shaft without changing its native shell. */
public final class ScriptedEncasedShaftBlock extends CustomEncasedShaftBlock implements BlockKJS {
    private static final Set<String> RESERVED = Set.of("axis", "waterlogged");
    private final BlockBuilder builder;
    private final CompanionBlockHooks hooks;
    public ScriptedEncasedShaftBlock(BlockBuilder builder, Supplier<Block> casing, ShaftBuilder shaft) {
        super(builder.createProperties(), casing, shaft);
        this.builder = builder;
        hooks = new CompanionBlockHooks(builder);
        registerDefaultState(hooks.defaultState(defaultBlockState()));
    }
    @Override public BlockBuilder kjs$getBlockBuilder() { return builder; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        CompanionBlockHooks.properties(properties, definition, RESERVED);
    }
    @Override public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter level, BlockPos pos, Player player) {
        return hooks.pick(level, pos, state, () -> super.getCloneItemStack(state, hit, level, pos, player));
    }
    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return hooks.pick(level, pos, state, () -> super.getCloneItemStack(level, pos, state));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return hooks.placement(context, super.getStateForPlacement(context)); }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) { return hooks.replace(state, context, () -> super.canBeReplaced(state, context)); }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) { hooks.step(level, pos, state, entity, () -> super.stepOn(level, pos, state, entity)); }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) { hooks.fall(level, pos, state, entity, distance, () -> super.fallOn(level, state, pos, entity, distance)); }
    @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) { hooks.bounce(level, entity, () -> super.updateEntityAfterFallOn(level, entity)); }
    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) { hooks.explode(level, pos, explosion, () -> super.wasExploded(level, pos, explosion)); }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return hooks.rotate(state, rotation, () -> super.rotate(state, rotation)); }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return hooks.mirror(state, mirror, () -> super.mirror(state, mirror)); }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return hooks.use(state, level, pos, player, hand, hit, () -> super.use(state, level, pos, player, hand, hit));
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { hooks.randomTick(state, level, pos, random, () -> super.randomTick(state, level, pos, random)); }
    @Override public boolean isRandomlyTicking(BlockState state) { return builder.randomTickCallback != null || super.isRandomlyTicking(state); }
    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return builder.transparent || super.propagatesSkylightDown(state, level, pos); }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return builder.transparent ? Shapes.empty() : super.getVisualShape(state, level, pos, context); }
    @Override public boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) { return builder.transparent && adjacent.is(this) || super.skipRendering(state, adjacent, direction); }
    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) { return builder.transparent ? 1 : super.getShadeBrightness(state, level, pos); }
}
