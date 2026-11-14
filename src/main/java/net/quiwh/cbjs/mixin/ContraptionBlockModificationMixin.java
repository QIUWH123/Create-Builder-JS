package net.quiwh.cbjs.mixin;

import dev.latvian.mods.kubejs.block.BlockModificationKubeEvent.BlockModifications;
import net.quiwh.cbjs.kubejs.ContraptionBlockModification;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockModifications.class, remap = false)
public abstract class ContraptionBlockModificationMixin implements ContraptionBlockModification {
}
