package net.quiwh.cbjs.mixin;

import com.simibubi.create.foundation.events.InputEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Chain connector clicks must not fall through to package pickup. */
@Mixin(value = InputEvents.class, remap = false)
public abstract class CustomChainInputMixin {
    @Group(name = "cbjs$chainInput", min = 1, max = 1)
    @Redirect(method = "onClickInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$chainInput(ItemStack stack, Item item) {
        return item == Items.CHAIN ? ChainMaterials.isChain(stack) : stack.is(item);
    }

    @Group(name = "cbjs$chainInput", min = 1, max = 1)
    @Redirect(method = "onClickInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_150930_(Lnet/minecraft/world/item/Item;)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$chainInputSrg(ItemStack stack, Item item) {
        return item == Items.CHAIN ? ChainMaterials.isChain(stack) : stack.is(item);
    }
}
