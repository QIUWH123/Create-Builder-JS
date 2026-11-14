package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlazeBurnerMovementBehaviour.class, remap = false)
public abstract class BlazeBurnerMovementEffectsMixin {
    @WrapOperation(method = "tick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")}, require = 1, allow = 1)
    private void cbjs$particlePermission(Level level, ParticleOptions particle, double x, double y, double z,
            double vx, double vy, double vz, Operation<Void> original, MovementContext context) {
        var builder = BlazeBurnerFamilies.builder(context.state);
        if (builder == null || builder.options().haveParticles) original.call(level, particle, x, y, z, vx, vy, vz);
    }
}
