package net.quiwh.cbjs.client;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedFluidVesselBlock;

/** Lateral metal follows the horizontal vessel; each surface retains its authored texture role. */
public final class FluidVesselCTBehaviour extends ConnectedTextureBehaviour.Base {
    private final CTSpriteShiftEntry side, end, inner;

    public FluidVesselCTBehaviour(CTSpriteShiftEntry side, CTSpriteShiftEntry end, CTSpriteShiftEntry inner) {
        this.side = side; this.end = end; this.inner = inner;
    }

    @Override public CTSpriteShiftEntry getShift(BlockState state, Direction face, TextureAtlasSprite sprite) {
        // A reveal can face along the vessel's axis while still carrying the side sheet.
        // Choose by sprite, since face normals alone cannot distinguish those surfaces.
        if (sprite == null || sprite == side.getOriginal()) return side;
        if (sprite == end.getOriginal()) return end;
        if (sprite == inner.getOriginal()) return inner;
        return null;
    }

    @Override public boolean buildContextForOccludedDirections() { return true; }

    @Override protected boolean reverseUVsVertically(BlockState state, Direction face) {
        // Follow the source-V reflection on every longitudinal wall, including roof and floor.
        return face.getAxis() != state.getValue(ScriptedFluidVesselBlock.AXIS);
    }

    @Override protected Direction getUpDirection(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis axis = state.getValue(ScriptedFluidVesselBlock.AXIS);
        if (axis == Direction.Axis.X && face.getAxis() == Direction.Axis.Y)
            return Direction.WEST;
        if (face.getAxis().isHorizontal() && face.getAxis() != axis)
            return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        return super.getUpDirection(world, pos, state, face);
    }

    @Override protected Direction getRightDirection(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction face) {
        Direction.Axis axis = state.getValue(ScriptedFluidVesselBlock.AXIS);
        // Base buildContext reverses this direction for positive face normals.
        if (axis == Direction.Axis.X && face.getAxis() == Direction.Axis.Y)
            return Direction.SOUTH;
        if (face.getAxis().isHorizontal() && face.getAxis() != axis)
            return axis == Direction.Axis.X ? Direction.UP : Direction.DOWN;
        return super.getRightDirection(world, pos, state, face);
    }

    @Override public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter world,
            BlockPos pos, BlockPos otherPos, Direction face) {
        return state.getBlock() == other.getBlock() && ConnectivityHandler.isConnected(world, pos, otherPos);
    }

}
