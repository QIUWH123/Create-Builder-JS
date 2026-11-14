package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.AllItems;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.item.*;
import net.quiwh.cbjs.content.ScriptedWrenchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractBogeyBlock.class, remap = false)
public abstract class EquipmentBogeyWrenchMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"),
            remap = false, require = 1, allow = 1)
    private boolean cbjs$recognizeWrench(ItemStack stack, Item expected, Operation<Boolean> original) {
        return cbjs$matchesWrench(stack, expected, original);
    }

    private static boolean cbjs$matchesWrench(ItemStack stack, Item expected, Operation<Boolean> original) {
        return stack.getItem() instanceof ScriptedWrenchItem && expected == AllItems.WRENCH.get()
                || original.call(stack, expected);
    }
}
