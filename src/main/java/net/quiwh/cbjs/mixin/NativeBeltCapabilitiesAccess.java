package net.quiwh.cbjs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses Create's native automation handlers for scripted block entity types. */
@Mixin(value = com.simibubi.create.content.kinetics.belt.BeltBlockEntity.class, remap = false)
public interface NativeBeltCapabilitiesAccess {
    @Accessor("itemHandler") net.neoforged.neoforge.items.IItemHandler cbjs$getItemHandler();
    @Invoker("initializeItemHandler") void cbjs$initializeItemHandler();
}
