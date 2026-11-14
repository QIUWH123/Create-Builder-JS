package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BeltInventory.class, remap = false)
public abstract class BeltActionInventoryMixin {
    @Shadow @Final BeltBlockEntity belt;
    @Inject(method = "addItem", at = @At("RETURN"), require = 1)
    private void cbjs$entered(TransportedItemStack item, CallbackInfo ci) {
        BeltEventDispatcher.entered(belt, item);
    }
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/belt/behaviour/DirectBeltInputBehaviour;handleInsertion(Lcom/simibubi/create/content/kinetics/belt/transport/TransportedItemStack;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private ItemStack cbjs$endpoint(DirectBeltInputBehaviour target, TransportedItemStack item,
                                         Direction direction, boolean simulate, Operation<ItemStack> original) {
        if (simulate || !BeltEventDispatcher.listens(CBJSEvents.BELT_ITEM_EXITED, belt))
            return original.call(target, item, direction, simulate);
        ItemStack before = item.stack.copy();
        ItemStack remaining = original.call(target, item, direction, false);
        BeltEventDispatcher.lost(belt, item.beltPosition, before, remaining, "insertion", target.getPos(), direction);
        return remaining;
    }
    @WrapOperation(method = "eject", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z")}, require = 1, allow = 1, remap = false)
    private boolean cbjs$ejected(Level level, Entity entity, Operation<Boolean> original,
                                      TransportedItemStack transported) {
        boolean success = original.call(level, entity);
        if (success && entity instanceof ItemEntity item)
            BeltEventDispatcher.exited(belt, transported.beltPosition, item.getItem(), "ejection", null, belt.getMovementFacing());
        return success;
    }
    // The second List.add marks the original consumed after all replacement outputs were queued.
    // Fillers may consume input before this callback; the scoped snapshot takes precedence.
    // Reuse Create's local for all other processing, without new copies on unchanged ticks.
    @WrapOperation(method = "applyToEachWithin", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", ordinal = 1), require = 1)
    private boolean cbjs$completedProcessing(List<TransportedItemStack> removals, Object removed, Operation<Boolean> original,
            @Local TransportedResult result, @Local ItemStack stackBefore) {
        boolean committed = original.call(removals, removed);
        if (committed && BeltEventDispatcher.observesChanges(belt)) {
            var transported = (TransportedItemStack) removed;
            var change = BeltEventDispatcher.capture(stackBefore, transported.beltPosition, result, transported);
            if (change != null) BeltEventDispatcher.committed(belt, change);
        }
        return committed;
    }
}
