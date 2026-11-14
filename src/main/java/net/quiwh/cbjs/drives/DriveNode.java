package net.quiwh.cbjs.drives;

import java.util.Objects;
import net.minecraft.core.BlockPos;

/** A wheel centre and its visible drive track radius, independent of its kinetic ratio. */
public record DriveNode(BlockPos pos, double radius) {
    public DriveNode {
        pos = Objects.requireNonNull(pos, "Drive node position").immutable();
        if (!Double.isFinite(radius) || radius <= 0 || radius > 1)
            throw new IllegalArgumentException("Drive radius must be finite and in (0, 1]");
    }
}
