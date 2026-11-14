package net.quiwh.cbjs.mixin;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import net.quiwh.cbjs.kubejs.ContraptionBlockBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BlockBuilder.class, remap = false)
public abstract class ContraptionBlockBuilderMixin implements ContraptionBlockBuilder {
    @Unique private int cbjs$contraptionFlags;
    @Override public int cbjs$contraptionFlags() { return cbjs$contraptionFlags; }
    @Override public void cbjs$contraptionFlags(int flags) { cbjs$contraptionFlags = flags; }
}
