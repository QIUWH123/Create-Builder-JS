package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LitBlazeBurnerBlock.class, remap = false)
public abstract class LitBlazeBurnerMaterialMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState cbjs$extinguishMaterial(BlockEntry<?> entry, Operation<BlockState> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        return builder == null ? original.call(entry) : net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, builder.get().defaultBlockState());
    }
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/processing/burner/LitBlazeBurnerBlock;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState cbjs$soulState(LitBlazeBurnerBlock block, Operation<BlockState> original, @Local(argsOnly = true) BlockState state) {
        BlockState target = original.call(block);
        return BlazeBurnerFamilies.builder(state) == null ? target : net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, target);
    }
    @WrapOperation(method = "useItemOn", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")}, require = 2, allow = 2)
    private void cbjs$interactionSound(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.core.BlockPos pos, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source,
            float volume, float pitch, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null || builder.options().haveSound) original.call(level, player, pos, sound, source, volume, pitch);
    }
}
