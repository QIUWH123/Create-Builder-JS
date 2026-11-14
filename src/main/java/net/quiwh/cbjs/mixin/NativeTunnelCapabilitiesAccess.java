package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses Create's lazy belt forwarding, including the existing custom-belt recognition hook. */
@Mixin(value = BeltTunnelBlockEntity.class, remap = false)
public interface NativeTunnelCapabilitiesAccess {
    @Invoker("lambda$registerCapabilities$0")
    static IItemHandler cbjs$getItemHandler(BeltTunnelBlockEntity entity, Direction side) {
        throw new AssertionError("Native tunnel capability invoker was not transformed");
    }
}
