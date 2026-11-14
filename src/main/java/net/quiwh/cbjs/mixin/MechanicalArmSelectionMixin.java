package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.MechanicalArmFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointHandler.class}, remap = false)
public abstract class MechanicalArmSelectionMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"), require = 1)
    private static boolean cbjs$customArm(BlockEntry<?> entry, ItemStack stack, Operation<Boolean> original) {
        return MechanicalArmFamilies.isArmItem(entry, stack) || original.call(entry, stack);
    }
}
