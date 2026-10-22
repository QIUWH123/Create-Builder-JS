package net.quiwh.cbjs.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Native comparator notifications restricted to the loaded exterior of a storage cuboid. */
@dev.latvian.mods.rhino.util.HideFromJS
public final class StorageComparatorUpdates {
    public static void notifySurface(Level level, BlockPos origin, int sizeX, int sizeY, int sizeZ) {
        if (level.isClientSide) return;
        BlockPos.MutableBlockPos source = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos target = new BlockPos.MutableBlockPos();
        // Preserve Create's Y/Z/X order, skipping entire interior runs rather than visiting their blocks.
        for (int y = 0; y < sizeY; y++) for (int z = 0; z < sizeZ; z++) {
            boolean rim = y == 0 || y == sizeY - 1 || z == 0 || z == sizeZ - 1;
            for (int x = 0; x < sizeX; x += rim || sizeX == 1 ? 1 : sizeX - 1) {
                source.setWithOffset(origin, x, y, z);
                if (!level.hasChunkAt(source)) continue;
                Block block = level.getBlockState(source).getBlock();
                if (y == 0) notifyFace(level, block, source, target, Direction.DOWN);
                if (y == sizeY - 1) notifyFace(level, block, source, target, Direction.UP);
                if (z == 0) notifyFace(level, block, source, target, Direction.NORTH);
                if (z == sizeZ - 1) notifyFace(level, block, source, target, Direction.SOUTH);
                if (x == 0) notifyFace(level, block, source, target, Direction.WEST);
                if (x == sizeX - 1) notifyFace(level, block, source, target, Direction.EAST);
            }
        }
    }

    private static void notifyFace(Level level, Block block, BlockPos source, BlockPos.MutableBlockPos target,
                                   Direction direction) {
        target.setWithOffset(source, direction);
        if (!level.hasChunkAt(target)) return;
        var state = level.getBlockState(target);
        state.onNeighborChange(level, target, source);
        if (!state.isRedstoneConductor(level, target)) return;
        target.move(direction);
        if (!level.hasChunkAt(target)) return;
        state = level.getBlockState(target);
        if (state.getWeakChanges(level, target)) level.neighborChanged(state, target, block, source, false);
    }

    private StorageComparatorUpdates() { }
}
