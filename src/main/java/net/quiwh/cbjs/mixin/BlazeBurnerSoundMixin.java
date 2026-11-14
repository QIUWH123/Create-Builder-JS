package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerSoundMixin {
    @WrapOperation(method = "tryUpdateFuel", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")}, require = 1, allow = 1)
    private void cbjs$ambientPermission(Level level, Player player, BlockPos pos, SoundEvent sound, SoundSource source,
            float volume, float pitch, Operation<Void> original) {
        var builder = BlazeBurnerFamilies.builder(((BlazeBurnerBlockEntity)(Object)this).getBlockState());
        if (builder == null || builder.options().haveSound) original.call(level, player, pos, sound, source, volume, pitch);
    }
}
