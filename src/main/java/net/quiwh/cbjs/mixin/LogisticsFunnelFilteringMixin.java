package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import net.quiwh.cbjs.content.ScriptedFunnelBlock;
import net.quiwh.cbjs.content.ScriptedBeltFunnelBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filtering is a brass behavior, independent of the script-selected registry identity. */
@Mixin(value = FunnelBlockEntity.class, remap = false)
public abstract class LogisticsFunnelFilteringMixin {
    @Inject(method = "supportsFiltering", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$filtering(CallbackInfoReturnable<Boolean> cir) {
        var block = ((FunnelBlockEntity) (Object) this).getBlockState().getBlock();
        if (block instanceof ScriptedFunnelBlock funnel) cir.setReturnValue(funnel.sourceBuilder().brass);
        else if (block instanceof ScriptedBeltFunnelBlock funnel) cir.setReturnValue(funnel.sourceBuilder().funnel.brass);
    }
}
