package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.deployer.DeployerMovingInteraction;
import com.simibubi.create.AllItems;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.quiwh.cbjs.content.ScriptedWrenchItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DeployerMovingInteraction.class, remap = false)
public abstract class EquipmentDeployerWrenchMixin {
    @WrapOperation(method = "handlePlayerInteraction", at = @At(value = "INVOKE",
            target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z", remap = false), remap = false)
    private boolean cbjs$recognizeWrench(Object held, Object expected, Operation<Boolean> original) {
        return held instanceof ScriptedWrenchItem && expected == AllItems.WRENCH.get()
                || original.call(held, expected);
    }
}
