package net.quiwh.cbjs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Retains native heat and catalyst conditions when this burner permits fan processing. */
@Mixin(targets = {"com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$BlastingType",
        "com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$SmokingType",
        "com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes$HauntingType"}, remap = false)
public abstract class BlazeBurnerFanProcessingMixin {
    @Inject(method = "isValidAt", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$fanPermission(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var builder = BlazeBurnerFamilies.builder(level.getBlockState(pos));
        if (builder != null && !builder.options().canProcessFan) cir.setReturnValue(false);
    }
}
