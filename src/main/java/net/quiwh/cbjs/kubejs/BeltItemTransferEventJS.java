package net.quiwh.cbjs.kubejs;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class BeltItemTransferEventJS extends BeltItemEventJS {
    private final String reason;
    private final BlockPos sourcePos;
    private final BlockPos destinationPos;
    private final Direction direction;
    public BeltItemTransferEventJS(Level level, BlockPos pos, BlockPos controller, Block block, ItemStack item,
                                  String reason, @Nullable BlockPos source, @Nullable BlockPos destination,
                                  @Nullable Direction direction) {
        super(level, pos, controller, block, item);
        this.reason = reason;
        sourcePos = source == null ? null : source.immutable();
        destinationPos = destination == null ? null : destination.immutable();
        this.direction = direction;
    }
    public String getReason() { return reason; }
    @Nullable public BlockPos getSourcePos() { return sourcePos; }
    @Nullable public BlockPos getDestinationPos() { return destinationPos; }
    @Nullable public String getDirection() { return direction == null ? null : direction.getName(); }
}
