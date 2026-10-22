package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Controller changes move existing stacks through addItem without inserting new items. */
@Mixin(value = BeltSlicer.class, remap = false)
public abstract class BeltActionSlicerMixin {
    @WrapOperation(method = {"useWrench", "useConnector"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltInventory;addItem(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;)V"),
            require = 5, allow = 5)
    private static void cbjs$migrate(BeltInventory inventory, TransportedItemStack item, Operation<Void> original) {
        if (!CBJSEvents.BELT_ITEM_ENTERED.hasListeners()) {
            original.call(inventory, item);
            return;
        }
        boolean previous = BeltEventDispatcher.pushMigration();
        try { original.call(inventory, item); }
        finally { BeltEventDispatcher.popMigration(previous); }
    }
}
