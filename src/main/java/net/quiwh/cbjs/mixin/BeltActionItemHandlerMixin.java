package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.ItemHandlerBeltSegment;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ItemHandlerBeltSegment.class, remap = false)
public abstract class BeltActionItemHandlerMixin {
    @Shadow @Final private BeltInventory beltInventory;
    @Shadow int offset;
    @Inject(method = "extractItem", at = @At("RETURN"), require = 1)
    private void cbjs$extracted(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        if (!simulate) BeltEventDispatcher.exited(((BeltInventoryAccess) beltInventory).cbjs$getBelt(), offset + .5f,
                cir.getReturnValue(), "extraction", null, null);
    }
}
