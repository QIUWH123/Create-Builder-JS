package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorConnectionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.compat.createchain.ConveyorChains;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChainConveyorConnectionPacket.class, remap = false)
public abstract class CustomChainConveyorPacketMixin {
    @Shadow private BlockPos targetPos;
    @Shadow private boolean connect;
    @Shadow private ItemStack chain;

    @Inject(method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$applyMaterial(ServerPlayer player, ChainConveyorBlockEntity entity, CallbackInfo ci) {
        ConveyorChains.apply(player, entity, targetPos, chain, connect);
        ci.cancel();
    }
}
