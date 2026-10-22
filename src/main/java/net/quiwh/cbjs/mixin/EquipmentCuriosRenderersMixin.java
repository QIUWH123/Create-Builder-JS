package net.quiwh.cbjs.mixin;

import net.quiwh.cbjs.client.ScriptedEquipmentClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loaded by Create only when Curios is installed. No Curios classes enter the common class path. */
@Mixin(targets = "com.simibubi.create.compat.curios.CuriosRenderers", remap = false)
public abstract class EquipmentCuriosRenderersMixin {
    @Inject(method = "register", at = @At("TAIL"), remap = false)
    private static void cbjs$registerCustomGoggles(CallbackInfo info) {
        ScriptedEquipmentClient.registerCuriosGoggles();
    }
}
