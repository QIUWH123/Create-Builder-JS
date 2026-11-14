package net.quiwh.cbjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses Create's native automation handlers for scripted block entity types. */
@Mixin(value = com.simibubi.create.content.logistics.chute.ChuteBlockEntity.class, remap = false)
public interface NativeChuteCapabilitiesAccess {
    @Accessor("itemHandler") com.simibubi.create.content.logistics.chute.ChuteItemHandler cbjs$getItemHandler();
}
