package net.quiwh.cbjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses Create's native automation handlers for scripted block entity types. */
@Mixin(value = com.simibubi.create.content.processing.basin.BasinBlockEntity.class, remap = false)
public interface NativeBasinCapabilitiesAccess {
    @Accessor("itemCapability") net.neoforged.neoforge.items.IItemHandlerModifiable cbjs$getItemCapability();
    @Accessor("fluidCapability") net.neoforged.neoforge.fluids.capability.IFluidHandler cbjs$getFluidCapability();
}
