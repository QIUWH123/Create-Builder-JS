package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.contraptions.Contraption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.kubejs.ContraptionRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Contraption.class, remap = false)
public abstract class ContraptionPlacementMixin {
    @Inject(method = "movementAllowed", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$linearMovement(BlockState state, Level world, BlockPos pos,
            CallbackInfoReturnable<Boolean> callback) {
        if (ContraptionRules.deniesLinearMovement((Contraption)(Object)this, state)) callback.setReturnValue(false);
    }

    @WrapOperation(method = "addBlocksToWorld", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;m_60800_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F")},
            require = 1, allow = 1)
    private float cbjs$protectWorldTarget(BlockState worldState, BlockGetter world, BlockPos pos,
            Operation<Float> original) {
        // Reuse Create's unbreakable-target fallback: keep the world block and drop the incoming member.
        return ContraptionRules.has(worldState, ContraptionRules.NO_SQUASH) ? -1 : original.call(worldState, world, pos);
    }
}
