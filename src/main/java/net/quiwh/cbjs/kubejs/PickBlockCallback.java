package net.quiwh.cbjs.kubejs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

/** The native Create pick stack remains the fallback when the script leaves item unset. */
public final class PickBlockCallback {
    public final BlockGetter level;
    public final BlockPos pos;
    public final BlockState state;
    public ItemLike item;

    public PickBlockCallback(BlockGetter level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }
}
