package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.quiwh.cbjs.compat.createchain.ConveyorChains;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChainConveyorBlock.class, remap = false)
public abstract class CustomChainConveyorBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$useChain(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!level.isClientSide && player != null && ChainMaterials.isChain(player.getItemInHand(hand)))
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
    }

    @Inject(method = "lambda$onSneakWrenched$1", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$refundEdges(Player player, ChainConveyorBlockEntity entity, CallbackInfo ci) {
        ConveyorChains.refundAll(player, entity);
        ci.cancel();
    }
}
