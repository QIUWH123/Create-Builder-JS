package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockModificationKubeEvent.BlockModifications;

/** The builder restrictions, applied to an existing block in BlockEvents.modification. */
public interface ContraptionBlockModification {
    default BlockModifications noContraptionAssembly() { return cbjs$restrict(ContraptionRules.NO_ASSEMBLY); }
    default BlockModifications noContraptionPush() { return cbjs$restrict(ContraptionRules.NO_PUSH); }
    default BlockModifications noContraptionSquash() { return cbjs$restrict(ContraptionRules.NO_SQUASH); }
    default BlockModifications noContraptionPickup() { return cbjs$restrict(ContraptionRules.NO_PICKUP); }

    private BlockModifications cbjs$restrict(int flag) {
        var modification = (BlockModifications)(Object)this;
        ContraptionRules.restrict(modification.block(), flag);
        return modification;
    }
}
