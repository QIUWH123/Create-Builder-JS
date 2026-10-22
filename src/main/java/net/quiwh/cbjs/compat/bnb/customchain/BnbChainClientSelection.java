package net.quiwh.cbjs.compat.bnb.customchain;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ChainMaterials;

/** The one in-progress native placement loop; material changes restart that loop. */
public final class BnbChainClientSelection {
    private static ResourceLocation material = ChainMaterials.VANILLA;
    public static InteractionHand selectedHand(Player player) {
        if (ChainMaterials.isChain(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (ChainMaterials.isChain(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }
    public static ResourceLocation material() { return material; }
    public static void material(ResourceLocation id) { material = id; }
    public static ItemStack stack() { return ChainMaterials.stack(material, 1); }
    private BnbChainClientSelection() { }
}
