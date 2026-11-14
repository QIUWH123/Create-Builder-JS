package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.logistics.tunnel.BrassTunnelBlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the same distribution-aware handler returned by Create's native provider. */
@Mixin(value = BrassTunnelBlockEntity.class, remap = false)
public interface NativeBrassTunnelCapabilitiesAccess {
    @Accessor("tunnelCapability") IItemHandler cbjs$getTunnelCapability();
}
