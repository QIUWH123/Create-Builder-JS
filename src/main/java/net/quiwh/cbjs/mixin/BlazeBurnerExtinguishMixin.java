package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.processing.burner.BlazeBurnerHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import net.quiwh.cbjs.content.ScriptedLitBlazeBurnerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlazeBurnerHandler.class, remap = false)
public abstract class BlazeBurnerExtinguishMixin {
    @WrapOperation(method = "extinguishLitBurners", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1, allow = 1)
    private static boolean cbjs$recognize(BlockEntry<?> entry, BlockState state, Operation<Boolean> original) {
        return state.getBlock() instanceof ScriptedLitBlazeBurnerBlock || original.call(entry, state);
    }
    @WrapOperation(method = "extinguishLitBurners", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, allow = 1)
    private static BlockState cbjs$material(BlockEntry<?> entry, Operation<BlockState> original, Level level, BlockPos pos, Direction direction) {
        BlockState state = level.getBlockState(pos);
        var builder = BlazeBurnerFamilies.builder(state);
        return builder == null ? original.call(entry) : net.quiwh.cbjs.kubejs.CompanionInheritance.transfer(state, builder.get().defaultBlockState());
    }
    @WrapOperation(method = "extinguishLitBurners", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V")}, require = 1, allow = 1)
    private static void cbjs$splashSound(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos,
            net.minecraft.sounds.SoundEvent sound, net.minecraft.sounds.SoundSource source, float volume, float pitch,
            Operation<Void> original) {
        var builder = BlazeBurnerFamilies.builder(level.getBlockState(pos));
        if (builder == null || builder.options().haveSound) original.call(level, player, pos, sound, source, volume, pitch);
    }
    @WrapOperation(method = "thrownEggsGetEatenByBurner", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/AllSoundEvents$SoundEntry;playOnServer(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Vec3i;)V"), require = 1, allow = 1)
    private static void cbjs$eggSound(com.simibubi.create.AllSoundEvents.SoundEntry sound, Level level, Vec3i pos,
            Operation<Void> original) {
        var builder = BlazeBurnerFamilies.builder(level.getBlockState(new BlockPos(pos)));
        if (builder == null || builder.options().haveSound) original.call(sound, level, pos);
    }
}
