package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.quiwh.cbjs.content.FluidTankCompat;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = WhistleBlockEntity.class, remap = false)
public abstract class FluidTankWhistleEntityMixin {
    @Inject(method = "getTank", at = @At("RETURN"), cancellable = true, require = 1)
    private void cbjs$disableSavedWhistle(CallbackInfoReturnable<FluidTankBlockEntity> cir) {
        var tank = cir.getReturnValue();
        if (tank != null && tank.getBlockState().getBlock() instanceof ScriptedFluidTankBlock)
            cir.setReturnValue(FluidTankCompat.steamSource(tank));
    }
}
