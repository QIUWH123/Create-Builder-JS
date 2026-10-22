package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.EquipmentItemCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create's usage predicates recognize scripted equipment without replacing exact item equality. */
@Mixin(value = ItemProviderEntry.class, remap = false)
public abstract class EquipmentItemEntryMixin {
    @Inject(method = "isIn", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$recognizeEquipment(ItemStack stack, CallbackInfoReturnable<Boolean> result) {
        if (EquipmentItemCompat.matchesCreateEntry(((ItemProviderEntry<?>)(Object)this).getId(), stack))
            result.setReturnValue(true);
    }
}
