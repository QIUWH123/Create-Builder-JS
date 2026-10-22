package net.quiwh.cbjs.content;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.quiwh.cbjs.kubejs.FluidTankBuilder;

import dev.latvian.mods.kubejs.block.*;
import dev.latvian.mods.kubejs.block.callbacks.*;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Forwards ordinary script block hooks while retaining Create's native tank interactions and multiblock lifecycle. */
public class ScriptedFluidTankBlock extends FluidTankBlock implements BlockKJS, SimpleWaterloggedBlock {
    private final FluidTankBuilder builder;
    private final VoxelShape customShape;
    public ScriptedFluidTankBlock(FluidTankBuilder builder) {
        super(builder.createProperties(), false);
        registerDefaultState(defaultBlockState().setValue(SHAPE,
                builder.defaultWindowFaces().anyGlass() ? Shape.WINDOW : Shape.PLAIN));
        if (defaultBlockState().hasProperty(BlockStateProperties.WATERLOGGED))
            registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
        this.builder = builder;
        // Match BasicBlockJS: startup shapes are fixed when the block is created.
        customShape = builder.customShape == null || builder.customShape.isEmpty()
                ? null : BlockBuilder.createShape(builder.customShape);
        if (builder.defaultStateModification != null) {
            var callback = new BlockStateModifyCallbackJS(defaultBlockState());
            if (safeCallback(builder.defaultStateModification, callback, "Fluid tank default state callback"))
                registerDefaultState(callback.getState());
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void copyProperty(BlockStateModifyCallbackJS callback, BlockState state, Property property) {
        callback.setValue(property, state.getValue(property));
    }
    private static <T> boolean safeCallback(java.util.function.Consumer<T> consumer, T callback, String message) {
        try { consumer.accept(callback); return true; }
        catch (Exception exception) { dev.latvian.mods.kubejs.util.ConsoleJS.SERVER.error(message, exception); return false; }
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult hit, BlockGetter level, BlockPos pos, Player player) {
        if (builder.pickBlockCallback != null) {
            var callback = new PickBlockCallbackJS(level, pos, state);
            if (safeCallback(builder.pickBlockCallback, callback, "Fluid tank pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return super.getCloneItemStack(state, hit, level, pos, player);
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (builder.pickBlockCallback != null) {
            var callback = new PickBlockCallbackJS(level, pos, state);
            if (safeCallback(builder.pickBlockCallback, callback, "Fluid tank pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return super.getCloneItemStack(level, pos, state);
    }
    @Override public BlockBuilder kjs$getBlockBuilder() { return builder; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        if (properties instanceof KubeJSBlockProperties scripted)
            for (Property<?> property : scripted.blockBuilder.blockStateProperties)
                if (!java.util.Set.of("top", "bottom", "shape").contains(property.getName())
                        && (!(this instanceof ScriptedFluidVesselBlock)
                            || !java.util.Set.of("axis").contains(property.getName())))
                    definition.add(property);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return customShape == null ? super.getShape(state, level, pos, context) : customShape;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && state.hasProperty(BlockStateProperties.WATERLOGGED))
            state = state.setValue(BlockStateProperties.WATERLOGGED,
                    context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
        if (state != null) state = placementState(context, state);
        if (state != null && builder.placementStateModification != null) {
            var callback = new BlockStateModifyPlacementCallbackJS(context, this);
            for (var property : state.getProperties()) copyProperty(callback, state, property);
            safeCallback(builder.placementStateModification, callback, "Fluid tank placement callback");
            return callback.getState();
        }
        return state;
    }
    protected BlockState placementState(BlockPlaceContext context, BlockState state) { return state; }

    @Override public FluidState getFluidState(BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
    @Override public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction,
            BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }
    @Override public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }
    @Override public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluid);
    }
    @Override public net.minecraft.world.item.ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        return state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? SimpleWaterloggedBlock.super.pickupBlock(level, pos, state) : net.minecraft.world.item.ItemStack.EMPTY;
    }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return builder.canBeReplacedFunction == null ? super.canBeReplaced(state, context)
                : builder.canBeReplacedFunction.test(new CanBeReplacedCallbackJS(context, state));
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (builder.stepOnCallback == null) super.stepOn(level, pos, state, entity);
        else safeCallback(builder.stepOnCallback, new EntitySteppedOnBlockCallbackJS(level, entity, pos, state), "Fluid tank step callback");
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (builder.fallOnCallback == null) super.fallOn(level, state, pos, entity, distance);
        else safeCallback(builder.fallOnCallback, new EntityFallenOnBlockCallbackJS(level, entity, pos, state, distance), "Fluid tank fall callback");
    }
    @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (builder.afterFallenOnCallback == null) { super.updateEntityAfterFallOn(level, entity); return; }
        var callback = new AfterEntityFallenOnBlockCallbackJS(level, entity);
        safeCallback(builder.afterFallenOnCallback, callback, "Fluid tank bounce callback");
        if (!callback.hasChangedVelocity()) super.updateEntityAfterFallOn(level, entity);
    }
    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (builder.explodedCallback == null) super.wasExploded(level, pos, explosion);
        else safeCallback(builder.explodedCallback, new BlockExplodedCallbackJS(level, pos, explosion), "Fluid tank explosion callback");
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        if (builder.rotateStateModification == null) return super.rotate(state, rotation);
        var callback = new BlockStateRotateCallbackJS(state, rotation);
        return safeCallback(builder.rotateStateModification, callback, "Fluid tank rotation callback") ? callback.getState() : super.rotate(state, rotation);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (builder.mirrorStateModification == null) return super.mirror(state, mirror);
        var callback = new BlockStateMirrorCallbackJS(state, mirror);
        return safeCallback(builder.mirrorStateModification, callback, "Fluid tank mirror callback") ? callback.getState() : super.mirror(state, mirror);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (builder.rightClick == null) return super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide) builder.rightClick.accept(new BlockRightClickedEventJS(player, hand, pos, hit.getDirection()));
        return InteractionResult.SUCCESS;
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (builder.randomTickCallback != null)
            safeCallback(builder.randomTickCallback, new RandomTickCallbackJS(new dev.latvian.mods.kubejs.level.BlockContainerJS(level, pos), random), "Fluid tank random tick callback");
        else super.randomTick(state, level, pos, random);
    }
    @Override public boolean isRandomlyTicking(BlockState state) { return builder.randomTickCallback != null || super.isRandomlyTicking(state); }
    @Override public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return Math.max(state.getLightEmission(), super.getLightEmission(state, level, pos));
    }
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

    @Override public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof ScriptedFluidTankBlockEntity tank) tank.cycleWindowFace(context.getClickedFace());
        return InteractionResult.SUCCESS;
    }

    public FluidTankBuilder sourceBuilder() { return builder; }

    @Override public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return net.quiwh.cbjs.kubejs.ModFluidTanks.type(builder.id);
    }
}
