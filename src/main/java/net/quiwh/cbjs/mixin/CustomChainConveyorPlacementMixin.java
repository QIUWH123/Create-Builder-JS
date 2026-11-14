package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native wheel clearance applies across all native and scripted conveyor materials. */
@Mixin(value = ChainConveyorBlock.class, remap = false)
public abstract class CustomChainConveyorPlacementMixin {
    @Inject(method = {"getStateForPlacement", "m_5573_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$familyClearance(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            if (context.getLevel().getBlockState(context.getClickedPos().offset(x, 0, z)).getBlock() instanceof ChainConveyorBlock) {
                cir.setReturnValue(null);
                return;
            }
    }
}
