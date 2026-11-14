package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.foundation.utility.BlockHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedShaftBlock;
import net.quiwh.cbjs.content.ScriptedPoweredShaftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PoweredShaftBlock.class, remap = false)
public abstract class SteamPoweredShaftMixin {
    @Inject(method = "getEquivalent", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cbjs$materialCompanion(BlockState state, CallbackInfoReturnable<BlockState> cir) {
        if (state.getBlock() instanceof ScriptedPoweredShaftBlock) cir.setReturnValue(state);
        else if (state.getBlock() instanceof ScriptedShaftBlock shaft && shaft.sourceBuilder().powered != null)
            cir.setReturnValue(BlockHelper.copyProperties(state, shaft.sourceBuilder().powered.get().defaultBlockState()));
    }
}
