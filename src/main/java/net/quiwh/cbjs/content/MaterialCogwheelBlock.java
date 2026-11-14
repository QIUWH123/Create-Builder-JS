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
    private static final VoxelShape[] SMALL_FLANGES = {
            Block.box(4, 2, 2, 12, 14, 14), Block.box(2, 4, 2, 14, 12, 14), Block.box(2, 2, 4, 14, 14, 12)
    };
    private static final VoxelShape[] LARGE_FLANGES = {
            Block.box(4, 0, 0, 12, 16, 16), Block.box(0, 4, 0, 16, 12, 16), Block.box(0, 0, 4, 16, 16, 12)
    };
    private static final VoxelShape[] FLANGE_SHAFTS = {
            Block.box(0, 5, 5, 16, 11, 11), Block.box(5, 0, 5, 11, 16, 11), Block.box(5, 5, 0, 11, 11, 16)
    };
    private static final VoxelShape[] SMALL_FLANGES_WITH_SHAFTS = addFlangeShafts(SMALL_FLANGES);
    private static final VoxelShape[] LARGE_FLANGES_WITH_SHAFTS = addFlangeShafts(LARGE_FLANGES);

    private static VoxelShape[] addFlangeShafts(VoxelShape[] plates) {
        VoxelShape[] shapes = new VoxelShape[3];
        for (int axis = 0; axis < shapes.length; axis++)
            shapes[axis] = net.minecraft.world.phys.shapes.Shapes.or(plates[axis], FLANGE_SHAFTS[axis]);
        return shapes;
    }

    private final GearModelSet models;
    private final boolean shaftless;
    private final boolean flanged;

    public MaterialCogwheelBlock(Properties properties, GearModelSet models, boolean large) {
        this(properties, models, large, false);
    }

    public MaterialCogwheelBlock(Properties properties, GearModelSet models, boolean large, boolean shaftless) {
        this(properties, models, large, shaftless, false);
    }

    public MaterialCogwheelBlock(Properties properties, GearModelSet models, boolean large,
                                 boolean shaftless, boolean flanged) {
        super(large, properties);
        this.models = models;
        this.shaftless = shaftless;
        this.flanged = flanged;
    }

    @Override
    public boolean isFlanged() {
        return flanged;
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
        int axis = state.getValue(AXIS).ordinal();
        if (flanged) {
            return (shaftless ? (isLargeCog() ? LARGE_FLANGES : SMALL_FLANGES)
                    : (isLargeCog() ? LARGE_FLANGES_WITH_SHAFTS : SMALL_FLANGES_WITH_SHAFTS))[axis];
        }
        return shaftless ? (isLargeCog() ? LARGE_PLATES : SMALL_PLATES)[axis]
                : super.getShape(state, level, pos, context);
    }

    @Override
    public BlockEntityType<? extends KineticBlockEntity> getBlockEntityType() {
        return ModBlockEntities.COGWHEEL.get();
    }
}
