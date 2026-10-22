package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChain;
import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelNode;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.compat.bnb.BnbChainedCogwheelBlock;
import net.quiwh.cbjs.compat.bnb.BnbChainedCogwheelBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CogwheelChain.class, remap = false)
public abstract class BnbCogwheelChainMixin {
    @Inject(method = "isValidChainCogwheel", at = @At("HEAD"), cancellable = true)
    private void cbjs$acceptCompanion(BlockState state, CallbackInfoReturnable<Boolean> callback) {
        if (state.getBlock() instanceof BnbChainedCogwheelBlock) callback.setReturnValue(true);
    }

    @WrapMethod(method = "placeChainCogwheelInLevel", remap = false)
    private void cbjs$saveSourceState(Level level, PlacingCogwheelNode node, boolean isController,
                                           int chainsUsed, BlockPos controllerPos, Operation<Void> original) {
        BlockState source = level.getBlockState(node.pos());
        original.call(level, node, isController, chainsUsed, controllerPos);
        if (level.getBlockEntity(node.pos()) instanceof BnbChainedCogwheelBlockEntity entity)
            entity.setSourceState(source);
    }

    @WrapMethod(method = "removeChainCogwheelFromLevelIfPresent", remap = false)
    private static void cbjs$restoreSourceState(Level level, BlockPos pos, Operation<Void> original) {
        if (level.getBlockEntity(pos) instanceof BnbChainedCogwheelBlockEntity entity
                && level.getBlockState(pos).getBlock() instanceof BnbChainedCogwheelBlock) {
            level.setBlockAndUpdate(pos, entity.sourceState());
            return;
        }
        original.call(level, pos);
    }
}
