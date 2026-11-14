package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Counts native accepted losses after each transport helper, including partial transfers. */
@Mixin(value = BeltInventory.class, remap = false)
public abstract class BeltActionInteractionMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltFunnelInteractionHandler;checkForFunnels(Lcom/simibubi/create/content/kinetics/belt/transport/BeltInventory;Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;F)Z"), require = 1)
    private boolean cbjs$funnel(BeltInventory inventory, TransportedItemStack item, float offset, Operation<Boolean> original) {
        return cbjs$observe(inventory, item, offset, original, "funnel");
    }
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltCrusherInteractionHandler;checkForCrushers(Lcom/simibubi/create/content/kinetics/belt/transport/BeltInventory;Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;F)Z"), require = 1)
    private boolean cbjs$crusher(BeltInventory inventory, TransportedItemStack item, float offset, Operation<Boolean> original) {
        return cbjs$observe(inventory, item, offset, original, "crusher");
    }
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/transport/BeltTunnelInteractionHandler;flapTunnelsAndCheckIfStuck(Lcom/simibubi/create/content/kinetics/belt/transport/BeltInventory;Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;F)Z"), require = 1)
    private boolean cbjs$tunnel(BeltInventory inventory, TransportedItemStack item, float offset, Operation<Boolean> original) {
        return cbjs$observe(inventory, item, offset, original, "tunnel");
    }
    @org.spongepowered.asm.mixin.Unique
    private boolean cbjs$observe(BeltInventory inventory, TransportedItemStack item, float offset,
                                      Operation<Boolean> original, String reason) {
        BeltBlockEntity belt = ((BeltInventoryAccess) inventory).cbjs$getBelt();
        if (!BeltEventDispatcher.listens(CBJSEvents.BELT_ITEM_EXITED, belt))
            return original.call(inventory, item, offset);
        ItemStack before = item.stack.copy();
        boolean result = original.call(inventory, item, offset);
        BeltEventDispatcher.lost(belt, item.beltPosition, before, item.stack, reason, null, null);
        return result;
    }
}
