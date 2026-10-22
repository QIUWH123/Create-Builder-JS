package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import net.quiwh.cbjs.kinetics.NetworkStressAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntity.class, remap = false)
public abstract class SmartBlockEntityStressMixin {
    @Inject(method = "onChunkUnloaded", at = @At("RETURN"), remap = false)
    private void cbjs$invalidateUnloadedDemand(CallbackInfo ci) {
        // Create skips KineticBlockEntity.remove() for chunk unloads.
        if ((Object) this instanceof KineticBlockEntity entity) NetworkStressAccess.invalidate(entity);
    }

    // Create's Minecraft override has the SRG name in a packaged Forge installation.
    @Inject(method = {"setRemoved()V", "m_7651_()V"},
            at = {@At("HEAD"), @At("RETURN")}, remap = false)
    private void cbjs$invalidateChunkRemoval(CallbackInfo ci) {
        // Forge first marks all entities as unloaded, then runs their removal callbacks, then
        // clears the chunk map. A callback may refresh demand, so invalidate again afterward.
        if ((Object) this instanceof KineticBlockEntity entity && entity.isChunkUnloaded()) {
            NetworkStressAccess.invalidate(entity);
        }
    }
}
