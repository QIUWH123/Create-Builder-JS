package net.quiwh.cbjs.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep native arm interaction point simulation and insertion for every scripted burner. */
@Mixin(targets = "com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes$BlazeBurnerType", remap = false)
public abstract class BlazeBurnerArmMixin {
    @Inject(method = "canCreatePoint", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$material(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof ScriptedBlazeBurnerBlock burner) cir.setReturnValue(burner.sourceBuilder().options().canBeFedByArm);
    }
}
