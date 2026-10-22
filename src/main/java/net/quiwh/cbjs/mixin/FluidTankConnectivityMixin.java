package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity;
import net.quiwh.cbjs.storage.StorageEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ConnectivityHandler.class, remap = false)
public abstract class FluidTankConnectivityMixin {
    @Inject(method = "splitMultiAndInvalidate", at = @At("HEAD"), require = 1, allow = 1)
    private static void cbjs$rememberFluidBeforeRedistribution(BlockEntity part, @Coerce Object cache,
            boolean tryReconnect, CallbackInfo callback) {
        StorageEventDispatcher.beforeFluidRedistribution(part);
    }

    @WrapOperation(method = "tryToFormNewMultiOfWidth", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/foundation/blockEntity/IMultiBlockEntityContainer;getWidth()I"),
            require = 1, allow = 1)
    private static int cbjs$deferSavedGeometry(IMultiBlockEntityContainer part, Operation<Integer> original) {
        // Reject the candidate before native merging transfers fluid or changes any member.
        return part instanceof ScriptedFluidTankBlockEntity tank && tank.connectivityMigrationPending()
                ? Integer.MAX_VALUE : original.call(part);
    }

    @Inject(method = "splitMulti", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private static void cbjs$deferUnsafeSplit(BlockEntity part, CallbackInfo callback) {
        // Native block removal must redistribute fluid into the surviving tanks.
        if (part.isRemoved()) return;
        if (part instanceof ScriptedFluidTankBlockEntity tank
                && tank.getControllerBE() instanceof ScriptedFluidTankBlockEntity controller
                && controller.connectivityMigrationPending()) callback.cancel();
    }

    @WrapOperation(method = {"tryToFormNewMultiOfWidth", "splitMultiAndInvalidate"},
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/IFluidTank;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I"),
            require = 2, allow = 2)
    private static int cbjs$preserveStoredFluid(IFluidTank tank, FluidStack stack,
            IFluidHandler.FluidAction action, Operation<Integer> original) {
        if (tank instanceof ScriptedFluidTankBlockEntity.Inventory inventory)
            return inventory.fillFromMultiblock(stack, action);
        return original.call(tank, stack, action);
    }
}
