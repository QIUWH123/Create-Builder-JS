package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MaterialCogwheelBlock extends CogWheelBlock implements MaterialGear {
    private static final VoxelShape[] SMALL_PLATES = {
            Block.box(6, 2, 2, 10, 14, 14), Block.box(2, 6, 2, 14, 10, 14), Block.box(2, 2, 6, 14, 14, 10)
    };
    private static final VoxelShape[] LARGE_PLATES = {
            Block.box(6, 0, 0, 10, 16, 16), Block.box(0, 6, 0, 16, 10, 16), Block.box(0, 0, 6, 16, 16, 10)
    };
    private final GearModelSet models;
    private final boolean shaftless;

    public MaterialCogwheelBlock(Properties properties, GearModelSet models, boolean large) {
        this(properties, models, large, false);
    }

    public MaterialCogwheelBlock(Properties properties, GearModelSet models, boolean large, boolean shaftless) {
        super(large, properties);
        this.models = models;
        this.shaftless = shaftless;
    }

    @Override
    public GearModelSet models() {
        return models;
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shaftless ? (isLargeCog() ? LARGE_PLATES : SMALL_PLATES)[state.getValue(AXIS).ordinal()]
                : super.getShape(state, level, pos, context);
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return ModBlockEntities.COGWHEEL.get();
    }
}
