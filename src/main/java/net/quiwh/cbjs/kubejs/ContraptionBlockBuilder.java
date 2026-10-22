package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.rhino.util.HideFromJS;

/** Fluent settings added to every KubeJS block builder by the common builder mixin. */
public interface ContraptionBlockBuilder {
    @HideFromJS int cbjs$contraptionFlags();
    @HideFromJS void cbjs$contraptionFlags(int flags);

    default BlockBuilder replaceTexture(String role, String path) { return ((BlockBuilder)this).texture(role, path); }

    default BlockBuilder noContraptionAssembly() { return cbjs$setContraptionFlag(ContraptionRules.NO_ASSEMBLY); }
    default BlockBuilder noContraptionPush() { return cbjs$setContraptionFlag(ContraptionRules.NO_PUSH); }
    default BlockBuilder noContraptionSquash() { return cbjs$setContraptionFlag(ContraptionRules.NO_SQUASH); }
    default BlockBuilder noContraptionPickup() { return cbjs$setContraptionFlag(ContraptionRules.NO_PICKUP); }

    private BlockBuilder cbjs$setContraptionFlag(int flag) {
        cbjs$contraptionFlags(cbjs$contraptionFlags() | flag);
        return (BlockBuilder)this;
    }
}
