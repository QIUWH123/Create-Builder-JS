package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.quiwh.cbjs.kubejs.MechanicalBeltBuilder;

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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Forwards ordinary script block hooks while retaining Create's native belt transport. */
public final class ScriptedMechanicalBeltBlock extends BeltBlock implements BlockKJS {
    private final MechanicalBeltBuilder builder;
    private final VoxelShape customShape;
    public ScriptedMechanicalBeltBlock(MechanicalBeltBuilder builder) {
        super(builder.createProperties());
        this.builder = builder;
        // Match BasicBlockJS: startup shapes are fixed when the block is created.
        customShape = builder.customShape == null || builder.customShape.isEmpty()
                ? null : BlockBuilder.createShape(builder.customShape);
        if (builder.defaultStateModification != null) {
            var callback = new BlockStateModifyCallbackJS(defaultBlockState());
            if (safeCallback(builder.defaultStateModification, callback, "Mechanical belt default state callback"))
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
            if (safeCallback(builder.pickBlockCallback, callback, "Mechanical belt pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return MechanicalBeltCompat.connector(state);
    }
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (builder.pickBlockCallback != null) {
            var callback = new PickBlockCallbackJS(level, pos, state);
            if (safeCallback(builder.pickBlockCallback, callback, "Mechanical belt pick block callback") && callback.item != null)
                return new net.minecraft.world.item.ItemStack(callback.item);
        }
        return MechanicalBeltCompat.connector(state);
    }
    @Override public BlockBuilder kjs$getBlockBuilder() { return builder; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        if (properties instanceof KubeJSBlockProperties scripted)
            for (Property<?> property : scripted.blockBuilder.blockStateProperties)
                if (!java.util.Set.of("facing", "slope", "part", "casing", "waterlogged").contains(property.getName()))
                    definition.add(property);
    }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return customShape == null ? super.getShape(state, level, pos, context) : customShape;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && builder.placementStateModification != null) {
            var callback = new BlockStateModifyPlacementCallbackJS(context, this);
            for (var property : state.getProperties()) copyProperty(callback, state, property);
            safeCallback(builder.placementStateModification, callback, "Mechanical belt placement callback");
            return callback.getState();
        }
        return state;
    }
    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return builder.canBeReplacedFunction == null ? super.canBeReplaced(state, context)
                : builder.canBeReplacedFunction.test(new CanBeReplacedCallbackJS(context, state));
    }
    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (builder.stepOnCallback == null) super.stepOn(level, pos, state, entity);
        else safeCallback(builder.stepOnCallback, new EntitySteppedOnBlockCallbackJS(level, entity, pos, state), "Mechanical belt step callback");
    }
    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (builder.fallOnCallback == null) super.fallOn(level, state, pos, entity, distance);
        else safeCallback(builder.fallOnCallback, new EntityFallenOnBlockCallbackJS(level, entity, pos, state, distance), "Mechanical belt fall callback");
    }
    @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (builder.afterFallenOnCallback == null) { super.updateEntityAfterFallOn(level, entity); return; }
        var callback = new AfterEntityFallenOnBlockCallbackJS(level, entity);
        safeCallback(builder.afterFallenOnCallback, callback, "Mechanical belt bounce callback");
        if (!callback.hasChangedVelocity()) super.updateEntityAfterFallOn(level, entity);
    }
    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (builder.explodedCallback == null) super.wasExploded(level, pos, explosion);
        else safeCallback(builder.explodedCallback, new BlockExplodedCallbackJS(level, pos, explosion), "Mechanical belt explosion callback");
    }
    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        if (builder.rotateStateModification == null) return super.rotate(state, rotation);
        var callback = new BlockStateRotateCallbackJS(state, rotation);
        return safeCallback(builder.rotateStateModification, callback, "Mechanical belt rotation callback") ? callback.getState() : super.rotate(state, rotation);
    }
    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (builder.mirrorStateModification == null) return super.mirror(state, mirror);
        var callback = new BlockStateMirrorCallbackJS(state, mirror);
        return safeCallback(builder.mirrorStateModification, callback, "Mechanical belt mirror callback") ? callback.getState() : super.mirror(state, mirror);
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (builder.rightClick == null) return super.use(state, level, pos, player, hand, hit);
        if (!level.isClientSide) builder.rightClick.accept(new BlockRightClickedEventJS(player, hand, pos, hit.getDirection()));
        return InteractionResult.SUCCESS;
    }
    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (builder.randomTickCallback != null)
            safeCallback(builder.randomTickCallback, new RandomTickCallbackJS(new dev.latvian.mods.kubejs.level.BlockContainerJS(level, pos), random), "Mechanical belt random tick callback");
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

    @Override public ItemRequirement getRequiredItems(BlockState state, BlockEntity entity) {
        var required = new java.util.ArrayList<ItemStack>();
        if (state.getValue(PART) != BeltPart.MIDDLE) required.add(MechanicalBeltShafts.shaftItem(MechanicalBeltShafts.get(entity)));
        if (state.getValue(PART) == BeltPart.START) required.add(MechanicalBeltCompat.connector(state));
        return required.isEmpty() ? ItemRequirement.NONE
                : new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, required);
    }

    public MechanicalBeltBuilder sourceBuilder() { return builder; }

    @Override public BlockEntityType<? extends BeltBlockEntity> getBlockEntityType() {
        return ScriptedBeltBlockEntities.BELT.get();
    }
}
