package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Both recipe and generic filling consume the input before notifying the belt inventory. */
@Mixin(value = {FillingBySpout.class, GenericItemFilling.class}, remap = false)
public abstract class BeltActionFillingMixin {
    @Inject(method = "fillItem", at = @At("HEAD"), require = 1)
    private static void cbjs$beforeFilling(Level level, int requiredAmount, ItemStack stack,
            FluidStack fluid, CallbackInfoReturnable<ItemStack> ci) {
        BeltEventDispatcher.captureProcessorInput(stack);
    }
}
