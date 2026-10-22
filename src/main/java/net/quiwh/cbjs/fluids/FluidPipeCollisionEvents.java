package net.quiwh.cbjs.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.FluidPipeCollisionEventJS;
import net.quiwh.cbjs.kubejs.CBJSEvents;

/** Hooks Create's existing collision path without polling networks or scanning pipes. */
public final class FluidPipeCollisionEvents {
    private FluidPipeCollisionEvents() {}

    public static boolean collisionCancelled(Level level, BlockPos pos, FluidStack fluid, FluidStack otherFluid) {
        var handler = CBJSEvents.FLUID_PIPE_COLLISION;
        if (!handler.hasListeners() || level.isClientSide) return false;
        BlockState state = level.getBlockState(pos);
        if (!FluidPipeEventDispatcher.isPipe(state.getBlock())) return false;
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockId == null || !handler.hasListeners(blockId)) return false;

        BlockEntity entity = level.getBlockEntity(pos);
        var event = new FluidPipeCollisionEventJS(level, pos, fluid, otherFluid);
        if (handler.post(event, blockId).interruptFalse()) return true;
        // A script may replace/remove the pipe. Its replacement must not receive this old reaction.
        return level.getBlockState(pos) != state || level.getBlockEntity(pos) != entity;
    }
}
