package net.quiwh.cbjs.mixin;

import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ScriptedBeltConnectorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.simibubi.create.content.equipment.toolbox.ToolboxInventory", remap = false)
public abstract class MechanicalBeltToolboxMixin {
    @Inject(method = "cleanItemNBT", at = @At("HEAD"), remap = false, require = 1)
    private static void cbjs$clearPulleySelection(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (stack.getItem() instanceof ScriptedBeltConnectorItem)
            stack.removeTagKey("FirstPulley");
    }

    @Inject(method = "canItemsShareCompartment", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void cbjs$keepMaterialCompartment(ItemStack first, ItemStack second,
                                                         CallbackInfoReturnable<Boolean> cir) {
        if (first.getItem() instanceof ScriptedBeltConnectorItem || second.getItem() instanceof ScriptedBeltConnectorItem)
            cir.setReturnValue(first.getItem() == second.getItem());
    }
}
