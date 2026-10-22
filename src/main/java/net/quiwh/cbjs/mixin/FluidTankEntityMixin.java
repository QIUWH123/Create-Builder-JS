package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FluidTankBlockEntity.class, remap = false)
public abstract class FluidTankEntityMixin {
    @WrapOperation(method = "read", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/fluids/tank/FluidTankBlockEntity;getCapacityMultiplier()I"),
            require = 2, allow = 2)
    private int cbjs$readMaterialCapacity(Operation<Integer> original) {
        var entity = (FluidTankBlockEntity)(Object)this;
        return entity.getBlockState().getBlock() instanceof ScriptedFluidTankBlock tank
                ? tank.sourceBuilder().capacityPerBlock() : original.call();
    }
}
