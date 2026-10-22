package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlockEntity;
import net.quiwh.cbjs.content.FluidTankCompat;
import net.quiwh.cbjs.content.ScriptedFluidTankBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SteamEngineBlockEntity.class, remap = false)
public abstract class FluidTankSteamEngineEntityMixin {
    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true, require = 1, allow = 1)
    private void cbjs$customTankValidity(CallbackInfoReturnable<Boolean> cir) {
        var engine = (SteamEngineBlockEntity)(Object)this;
        var level = engine.getLevel();
        if (level == null) return;
        var direction = SteamEngineBlock.getConnectedDirection(engine.getBlockState()).getOpposite();
        var state = level.getBlockState(engine.getBlockPos().relative(direction));
        if (state.getBlock() instanceof ScriptedFluidTankBlock)
            cir.setReturnValue(FluidTankCompat.steamAllowed(state, false));
    }

    @Inject(method = "getTank", at = @At("RETURN"), cancellable = true, require = 1)
    private void cbjs$disableSavedEngine(CallbackInfoReturnable<FluidTankBlockEntity> cir) {
        var tank = cir.getReturnValue();
        if (tank != null && tank.getBlockState().getBlock() instanceof ScriptedFluidTankBlock)
            cir.setReturnValue(FluidTankCompat.steamSource(tank));
    }
}
