package net.quiwh.cbjs.content;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.quiwh.cbjs.kubejs.FluidTankBuilder;

/** Horizontal Create tank family with independently registered block and entity identities. */
public final class ScriptedFluidVesselBlock extends ScriptedFluidTankBlock {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public ScriptedFluidVesselBlock(FluidTankBuilder builder) { super(builder); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> definition) {
        super.createBlockStateDefinition(definition);
        definition.add(AXIS);
    }

    @Override protected BlockState placementState(BlockPlaceContext context, BlockState state) {
        Direction.Axis axis = context.getHorizontalDirection().getAxis();
        BlockState support = context.getLevel().getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite()));
        if (support.is(this)) axis = support.getValue(AXIS);
        return state.setValue(AXIS, axis);
    }

    @Override public BlockState rotate(BlockState state, Rotation rotation) {
        if (sourceBuilder().rotateStateModification != null) return super.rotate(state, rotation);
        return transform(state, rotation::rotate);
    }

    @Override public BlockState mirror(BlockState state, Mirror mirror) {
        if (sourceBuilder().mirrorStateModification != null) return super.mirror(state, mirror);
        return transform(state, mirror::mirror);
    }

    private BlockState transform(BlockState state, java.util.function.UnaryOperator<Direction> transform) {
        Direction.Axis oldAxis = state.getValue(AXIS);
        Direction positive = transform.apply(Direction.get(Direction.AxisDirection.POSITIVE, oldAxis));
        Direction.Axis axis = positive.getAxis();
        BlockState result = state.setValue(AXIS, axis);
        if (positive.getAxisDirection() == Direction.AxisDirection.NEGATIVE)
            result = result.setValue(TOP, state.getValue(BOTTOM)).setValue(BOTTOM, state.getValue(TOP));
        return result;
    }
}
