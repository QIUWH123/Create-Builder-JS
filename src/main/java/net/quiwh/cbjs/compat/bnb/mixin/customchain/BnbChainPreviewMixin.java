package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.item.CogwheelChainPlacementEffect;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainClientSelection;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CogwheelChainPlacementEffect.class, remap = false)
public abstract class BnbChainPreviewMixin {
    @Inject(method = "isChain", at = @At("HEAD"), cancellable = true)
    private static void cbjs$acceptMaterial(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(ChainMaterials.isChain(stack)
                && ChainMaterials.id(stack).equals(BnbChainClientSelection.material()));
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;getChainsFromInventory(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;IZ)Z"))
    private static boolean cbjs$countChosenMaterial(Player player, ItemStack vanilla, int count, boolean simulate) {
        return ChainConveyorBlockEntity.getChainsFromInventory(player, BnbChainClientSelection.stack(), count, simulate);
    }
    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/equipment/blueprint/BlueprintOverlayRenderer;displayChainRequirements(Lnet/minecraft/world/item/Item;IZ)V"), index = 0)
    private static Item cbjs$showChosenMaterial(Item vanilla) { return BnbChainClientSelection.stack().getItem(); }
}
