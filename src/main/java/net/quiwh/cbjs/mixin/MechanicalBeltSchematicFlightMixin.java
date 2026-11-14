package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.schematics.cannon.LaunchedItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.quiwh.cbjs.content.MechanicalBeltLengths;

@Mixin(value = LaunchedItem.ForBelt.class, remap = false)
public abstract class MechanicalBeltSchematicFlightMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$limitSchematicLength(Level level, CallbackInfo callback) {
        var flight = (LaunchedItem.ForBelt)(Object)this;
        if (!MechanicalBeltLengths.allows(flight.state, flight.length)) callback.cancel();
    }

    @Redirect(method = "place", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;createBelts(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"),
            remap = false, require = 1, allow = 1)
    private void cbjs$placeStoredMaterial(Level level, BlockPos start, BlockPos end) {
        MechanicalBeltCompat.createBelts(level, start, end, ((LaunchedItem.ForBelt)(Object)this).state);
    }
}
