package net.quiwh.cbjs.mixin;

import com.simibubi.create.api.contraption.ContraptionMovementSetting;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.kubejs.ContraptionRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContraptionMovementSetting.class, remap = false)
public abstract class ContraptionMovementSettingMixin {
    @Inject(method = "get(Lnet/minecraft/world/level/block/Block;)Lcom/simibubi/create/api/contraption/ContraptionMovementSetting;",
            at = @At("RETURN"), cancellable = true, require = 1)
    private static void cbjs$scriptRestriction(Block block, CallbackInfoReturnable<ContraptionMovementSetting> callback) {
        var scripted = ContraptionRules.movementSetting(block);
        // A pickup rule must not relax an existing UNMOVABLE restriction.
        if (scripted != null && callback.getReturnValue() != ContraptionMovementSetting.UNMOVABLE)
            callback.setReturnValue(scripted);
    }
}
