package net.quiwh.cbjs.mixin;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.content.contraptions.MountedStorageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.quiwh.cbjs.content.MountedItemStorageCapacity;
import net.quiwh.cbjs.content.LegacyMountedStorageIds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = MountedStorageManager.class, remap = false)
public abstract class MountedStorageCapacityMixin {
    @ModifyVariable(method = "read", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 1, allow = 1)
    private CompoundTag cbjs$upgradeLegacyStorageIds(CompoundTag data) {
        return LegacyMountedStorageIds.upgrade(data);
    }

    @WrapOperation(method = "initialize", at = @At(value = "NEW",
            target = "(Lcom/google/common/collect/ImmutableMap;)Lcom/simibubi/create/api/contraption/storage/item/MountedItemStorageWrapper;"),
            require = 2, allow = 2)
    private MountedItemStorageWrapper cbjs$dynamicMountedCapacity(ImmutableMap<BlockPos, MountedItemStorage> storages,
            Operation<MountedItemStorageWrapper> original) {
        return MountedItemStorageCapacity.containsScripted(storages)
                ? new MountedItemStorageCapacity.Mounted(storages) : original.call(storages);
    }

    @WrapOperation(method = "attachExternal", at = @At(value = "NEW",
            target = "([Lnet/minecraftforge/items/IItemHandlerModifiable;)Lnet/minecraftforge/items/wrapper/CombinedInvWrapper;"),
            require = 1, allow = 1)
    private CombinedInvWrapper cbjs$dynamicExternalOffsets(IItemHandlerModifiable[] handlers,
            Operation<CombinedInvWrapper> original) {
        return MountedItemStorageCapacity.containsDynamic(handlers)
                ? new MountedItemStorageCapacity.Combined(handlers) : original.call((Object)handlers);
    }
}
