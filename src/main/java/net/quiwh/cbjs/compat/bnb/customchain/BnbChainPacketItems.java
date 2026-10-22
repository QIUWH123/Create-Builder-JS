package net.quiwh.cbjs.compat.bnb.customchain;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ChainMaterials;

/** Never trust a client hand ordinal or arbitrary held item as a chain material. */
public final class BnbChainPacketItems {
    public static ItemStack heldChain(Player player, int hand) {
        if (player == null || hand < 0 || hand >= InteractionHand.values().length) return ItemStack.EMPTY;
        ItemStack stack = player.getItemInHand(InteractionHand.values()[hand]);
        return ChainMaterials.isChain(stack) ? stack : ItemStack.EMPTY;
    }
    private BnbChainPacketItems() { }
}
