package net.quiwh.cbjs.compat.bnb.mixin;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainCandidate;
import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve the script opt-in even though native BnB now accepts every ICogWheel. */
@Mixin(value = CogwheelChainCandidate.class, remap = false)
public abstract class BnbChainCandidateMixin {
    @Inject(method = "isValidCandidate(Lnet/minecraft/world/level/block/Block;)Z", at = @At("HEAD"), cancellable = true)
    private static void cbjs$driveBoundary(Block block, CallbackInfoReturnable<Boolean> callback) {
        if (block instanceof MaterialGear gear && gear.isEncased()) {
            callback.setReturnValue(false);
            return;
        }
        if (block instanceof BlockKJS scripted && scripted.kjs$getBlockBuilder() instanceof CogwheelBuilder builder
                && !builder.anyDriveEnabled()) callback.setReturnValue(false);
    }
}
