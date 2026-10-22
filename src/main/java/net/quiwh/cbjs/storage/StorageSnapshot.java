package net.quiwh.cbjs.storage;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A captured controller identity; structure removal can outlive the original block entity. */
public record StorageSnapshot(Level level, BlockPos pos, BlockState state, StorageDimensions dimensions) {
    public static StorageSnapshot of(BlockEntity entity) {
        return new StorageSnapshot(entity.getLevel(), entity.getBlockPos().immutable(), entity.getBlockState(),
                StorageDimensions.of((IMultiBlockEntityContainer) entity));
    }
}
