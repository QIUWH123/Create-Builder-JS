package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlazeBurnerBlock.class, remap = false)
public abstract class BlazeBurnerMaterialMixin {
    @Inject(method = "getLitOrUnlitStack", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cbjs$materialItem(BlockState state, CallbackInfoReturnable<ItemStack> cir) {
        if (BlazeBurnerFamilies.builder(state) != null) cir.setReturnValue(BlazeBurnerFamilies.stack(state));
    }
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private BlockState cbjs$lightMaterial(BlockEntry<?> entry, Operation<BlockState> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        return builder == null ? original.call(entry) : net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, builder.litBlock().defaultBlockState());
    }
    @WrapOperation(method = "useItemOn", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")}, require = 1, allow = 1)
    private void cbjs$lightingSound(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.core.BlockPos pos, net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source,
            float volume, float pitch, Operation<Void> original, @Local(argsOnly = true) BlockState state) {
        var builder = BlazeBurnerFamilies.builder(state);
        if (builder == null || builder.options().haveSound) original.call(level, player, pos, sound, source, volume, pitch);
    }
}
