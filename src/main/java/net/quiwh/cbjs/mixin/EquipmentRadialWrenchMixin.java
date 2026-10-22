package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.world.item.*;
import net.quiwh.cbjs.content.ScriptedWrenchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

@Mixin(value = RadialWrenchHandler.class, remap = false)
public abstract class EquipmentRadialWrenchMixin {
    @Group(name = "cbjs$radialWrench", min = 1, max = 1)
    @WrapOperation(method = "onKeyInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getItem()Lnet/minecraft/world/item/Item;"),
            remap = false, require = 0, allow = 1)
    private static Item cbjs$recognizeWrench(ItemStack stack, Operation<Item> original) {
        return cbjs$wrenchItem(original.call(stack));
    }

    @Group(name = "cbjs$radialWrench", min = 1, max = 1)
    @WrapOperation(method = "onKeyInput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_41720_()Lnet/minecraft/world/item/Item;"),
            remap = false, require = 0, allow = 1)
    private static Item cbjs$recognizeWrenchSrg(ItemStack stack, Operation<Item> original) {
        return cbjs$wrenchItem(original.call(stack));
    }

    private static Item cbjs$wrenchItem(Item held) {
        return held instanceof ScriptedWrenchItem ? AllItems.WRENCH.get() : held;
    }
}
