package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.waterwheel.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedLargeWaterWheelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native shared footprint blocks proxy their actual custom master, including clone and flow ticks. */
@Mixin(value = WaterWheelStructuralBlock.class, remap = false)
public abstract class GeneratorWheelStructureMixin {
    @Inject(method = {"getCloneItemStack", "m_7397_"}, at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$clone(BlockState state, net.minecraft.world.phys.HitResult target, LevelReader level, BlockPos pos, net.minecraft.world.entity.player.Player player, CallbackInfoReturnable<ItemStack> ci) {
        var masterPos = WaterWheelStructuralBlock.getMaster(level, pos, state);
        var master = level.getBlockState(masterPos);
        if (master.getBlock() instanceof ScriptedLargeWaterWheelBlock)
            ci.setReturnValue(master.getBlock().getCloneItemStack(master, target, level, masterPos, player));
    }
    @WrapOperation(method = {"updateShape", "m_7417_"}, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;m_186460_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;I)V")}, require = 1, allow = 1)
    private void cbjs$schedule(LevelAccessor level, BlockPos pos, Block nativeBlock, int delay, Operation<Void> original) {
        Block actual = level.getBlockState(pos).getBlock();
        original.call(level, pos, actual instanceof ScriptedLargeWaterWheelBlock ? actual : nativeBlock, delay);
    }
}
