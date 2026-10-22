package net.quiwh.cbjs.compat.bnb;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import com.kipti.bnb.content.cogwheel_chain.block.ConnectingCogwheelChainBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.KubeJSBlockProperties;
import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.content.GearModelSet;
import net.quiwh.cbjs.content.MaterialCogwheelBlock;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;

/** Retains the source gear's appearance and shaft topology while BnB owns the chain. */
public final class BnbChainedCogwheelBlock extends ConnectingCogwheelChainBlock
        implements MaterialGear, BlockKJS, ProperWaterloggedBlock {
    private final CogwheelBuilder source;

    public BnbChainedCogwheelBlock(CogwheelBuilder source) {
        super(source.large, source.createProperties().dropsLike(source.get()),
                () -> new BlockEntry<>(CBJS.REGISTRATE, RegistryObject.create(source.id, ForgeRegistries.BLOCKS)));
        this.source = source;
        registerDefaultState(withPropertiesOf(source.get().defaultBlockState()));
    }

    public MaterialCogwheelBlock sourceBlock() { return (MaterialCogwheelBlock) source.get(); }

    public BlockState sourceState(BlockState chainedState) {
        return sourceBlock().withPropertiesOf(chainedState);
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        // Block's constructor invokes this before our fields are initialized. The original
        // KubeJS properties already identify the complete, constructed source definition.
        if (properties instanceof KubeJSBlockProperties scripted) {
            for (var property : scripted.blockBuilder.get().getStateDefinition().getProperties())
                if (!property.getName().equals("axis")) definition.add(property);
        }
    }

    @Override public BlockBuilder kjs$getBlockBuilder() { return source; }
    @Override public GearModelSet models() { return source.models(); }
    @Override public boolean isShaftless() { return source.shaftless; }

    @Override public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return !isShaftless() && super.hasShaftTowards(level, pos, state, face);
    }

    @Override public FluidState getFluidState(BlockState state) { return fluidState(state); }

    @Override public BlockState updateShape(BlockState state, Direction face, BlockState adjacent,
                                            LevelAccessor level, BlockPos pos, BlockPos adjacentPos) {
        updateWater(level, state, pos);
        return super.updateShape(state, face, adjacent, level, pos, adjacentPos);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withWater(super.getStateForPlacement(context), context);
    }

    @Override public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos,
                                           PathComputationType type) {
        return false;
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return sourceBlock().getShape(sourceState(state), level, pos, context);
    }

    @Override public ItemStack getCloneItemStack(BlockState state, HitResult hit, BlockGetter level,
                                                BlockPos pos, Player player) {
        return sourceBlock().getCloneItemStack(sourceState(state), hit, level, pos, player);
    }

    @Override public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return sourceBlock().getCloneItemStack(level, pos, sourceState(state));
    }

    @Override public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return source.canBeReplacedFunction == null ? super.canBeReplaced(state, context)
                : sourceBlock().canBeReplaced(sourceState(state), context);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, BlockHitResult hit) {
        return source.rightClick == null ? super.use(state, level, pos, player, hand, hit)
                : sourceBlock().use(sourceState(state), level, pos, player, hand, hit);
    }

    @Override public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        sourceBlock().randomTick(sourceState(state), level, pos, random);
    }

    @Override public boolean isRandomlyTicking(BlockState state) {
        return sourceBlock().isRandomlyTicking(sourceState(state));
    }

    @Override public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        sourceBlock().stepOn(level, pos, sourceState(state), entity);
    }

    @Override public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        sourceBlock().fallOn(level, sourceState(state), pos, entity, distance);
    }

    @Override public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        sourceBlock().updateEntityAfterFallOn(level, entity);
    }

    @Override public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        sourceBlock().wasExploded(level, pos, explosion);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        if (source.rotateStateModification == null) return super.rotate(state, rotation);
        return fromSourceResult(sourceBlock().rotate(sourceState(state), rotation));
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (source.mirrorStateModification == null) return super.mirror(state, mirror);
        return fromSourceResult(sourceBlock().mirror(sourceState(state), mirror));
    }

    private BlockState fromSourceResult(BlockState state) {
        return state.getBlock() == sourceBlock() ? withPropertiesOf(state) : state;
    }

    @Override public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return sourceBlock().propagatesSkylightDown(sourceState(state), level, pos);
    }

    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos,
                                               CollisionContext context) {
        return sourceBlock().getVisualShape(sourceState(state), level, pos, context);
    }

    @Override public boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return sourceBlock().skipRendering(sourceState(state), adjacent.getBlock() == this
                ? sourceState(adjacent) : adjacent, direction);
    }

    @Override public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return sourceBlock().getShadeBrightness(sourceState(state), level, pos);
    }

    @Override public BlockEntityType<? extends CogwheelChainBlockEntity> getBlockEntityType() {
        return BnbChainBlockEntities.COGWHEEL_CHAIN.get();
    }
}
