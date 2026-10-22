package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.item.CogwheelChainPlacementInteraction;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.event.InputEvent;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainClientSelection;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CogwheelChainPlacementInteraction.class, remap = false)
public abstract class BnbChainInteractionMixin {
    @Shadow protected static PlacingCogwheelChain currentBuildingChain;
    @Shadow protected static ResourceKey<Level> currentChainLevel;

    @Inject(method = "onRightClick", at = @At("HEAD"))
    private static void cbjs$restartOnMaterialChange(InputEvent.InteractionKeyMappingTriggered event,
                                                         CallbackInfoReturnable<Boolean> callback) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var hand = BnbChainClientSelection.selectedHand(player);
        if (hand == null) return;
        var id = ChainMaterials.id(player.getItemInHand(hand));
        if (currentBuildingChain != null && !id.equals(BnbChainClientSelection.material())) {
            currentBuildingChain = null;
            currentChainLevel = null;
        }
        BnbChainClientSelection.material(id);
    }

    @Group(name = "cbjs$chainSelection", min = 2, max = 2)
    @Redirect(method = "onRightClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private static boolean cbjs$selectChain(ItemStack stack, Item ignored) { return ChainMaterials.isChain(stack); }
    @Group(name = "cbjs$chainSelection", min = 2, max = 2)
    @Redirect(method = "onRightClick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_150930_(Lnet/minecraft/world/item/Item;)Z"), require = 0)
    private static boolean cbjs$selectChainSrg(ItemStack stack, Item ignored) { return ChainMaterials.isChain(stack); }

    @ModifyArg(method = "onRightClick", at = @At(value = "INVOKE",
            target = "Lcom/kipti/bnb/network/packets/from_client/PlaceCogwheelChainPacket;<init>(Lcom/kipti/bnb/content/cogwheel_chain/graph/PlacingCogwheelChain;I)V"), index = 1)
    private static int cbjs$sendSelectedHand(int nativeHand) {
        var player = Minecraft.getInstance().player;
        var hand = player == null ? null : BnbChainClientSelection.selectedHand(player);
        return hand == null ? nativeHand : hand.ordinal();
    }
}
