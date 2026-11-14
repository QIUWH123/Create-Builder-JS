package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.schematics.SchematicPrinter;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedMechanicalArmBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SchematicPrinter.class, remap = false)
public abstract class MechanicalArmSchematicMixin {
    @Inject(method = "shouldDeferBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private static void cbjs$deferCustomArm(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof ScriptedMechanicalArmBlock) cir.setReturnValue(true);
    }
}
