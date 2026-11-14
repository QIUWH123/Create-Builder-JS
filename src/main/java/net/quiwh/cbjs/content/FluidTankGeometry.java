package net.quiwh.cbjs.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Shared coordinates for Create's square cross section and longitudinal length. */
public final class FluidTankGeometry {
    public static BlockPos offset(Direction.Axis axis, int length, int a, int b) {
        return switch (axis) {
            case X -> new BlockPos(length, a, b);
            case Y -> new BlockPos(a, length, b);
            case Z -> new BlockPos(a, b, length);
        };
    }

    public static int extent(Direction.Axis main, Direction.Axis dimension, int width, int length) {
        return main == dimension ? length : width;
    }

    /** Model rotations: X90/Y90 for X vessels, X270 for Z vessels. */
    public static Direction modelDirection(Direction.Axis axis, Direction local) {
        if (axis == Direction.Axis.Y) return local;
        int x = local.getStepX(), y = local.getStepY(), z = local.getStepZ();
        return axis == Direction.Axis.X ? Direction.fromDelta(y, z, x) : Direction.fromDelta(x, -z, y);
    }

    public static Direction localDirection(Direction.Axis axis, Direction world) {
        for (Direction local : Direction.values()) if (modelDirection(axis, local) == world) return local;
        throw new IllegalArgumentException("Unknown direction " + world);
    }

    private FluidTankGeometry() { }
}
