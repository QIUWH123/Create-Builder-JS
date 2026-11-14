package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.quiwh.cbjs.kinetics.GearLimitManager;
import net.quiwh.cbjs.kinetics.RotationSpeedPolicy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {
    @ModifyVariable(
            method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At("STORE"), ordinal = 1, require = 1, allow = 1, remap = false)
    private static boolean cbjs$useDestinationSpeedLimits(boolean tooFast, KineticBlockEntity currentTE,
            @Local(ordinal = 1) KineticBlockEntity neighbourTE,
            @Local(ordinal = 2) float newSpeed, @Local(ordinal = 3) float oppositeSpeed) {
        if (!tooFast) {
            return false;
        }
        int globalMaxRpm = AllConfigs.server().kinetics.maxRotationSpeed.get();
        // Forward speed belongs to the neighbour; reverse speed belongs to the current block.
        // Rated RPM, stress and replacement drops stay in GearLimitManager's tick check.
        return RotationSpeedPolicy.exceedsCreateLimit(newSpeed, globalMaxRpm,
                GearLimitManager.usesIndependentRpmLimit(neighbourTE.getBlockState().getBlock()))
                || RotationSpeedPolicy.exceedsCreateLimit(oppositeSpeed, globalMaxRpm,
                GearLimitManager.usesIndependentRpmLimit(currentTE.getBlockState().getBlock()));
    }
}
