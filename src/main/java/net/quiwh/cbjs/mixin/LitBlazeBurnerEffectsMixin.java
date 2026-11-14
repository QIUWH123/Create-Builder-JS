package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Independent sound and particle controls for generated fire-lit companions. */
@Mixin(value = LitBlazeBurnerBlock.class, remap = false)
public abstract class LitBlazeBurnerEffectsMixin {
    @WrapOperation(method = "animateTick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")}, require = 2, allow = 2)
    private void cbjs$particles(Level level, ParticleOptions particle, double x, double y, double z,
            double vx, double vy, double vz, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null || builder.options().haveParticles) original.call(level, particle, x, y, z, vx, vy, vz);
    }
    @WrapOperation(method = "animateTick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addAlwaysVisibleParticle(Lnet/minecraft/core/particles/ParticleOptions;ZDDDDDD)V")}, require = 1, allow = 1)
    private void cbjs$smoke(Level level, ParticleOptions particle, boolean ignoreRange, double x, double y, double z,
            double vx, double vy, double vz, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null || builder.options().haveParticles) original.call(level, particle, ignoreRange, x, y, z, vx, vy, vz);
    }
    @WrapOperation(method = "animateTick", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V")}, require = 1, allow = 1)
    private void cbjs$sound(Level level, double x, double y, double z, SoundEvent sound, SoundSource source,
            float volume, float pitch, boolean delay, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null || builder.options().haveSound) original.call(level, x, y, z, sound, source, volume, pitch, delay);
    }
}
