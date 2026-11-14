package net.quiwh.cbjs.content;

import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kubejs.FunnelBuilder;

/** Native belt funnels require a BlockEntry parent; resolve the startup builder lazily. */
final class LogisticsFunnelEntry extends BlockEntry<FunnelBlock> {
    private final FunnelBuilder source;
    LogisticsFunnelEntry(FunnelBuilder source) {
        super(CBJS.REGISTRATE, net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.BLOCK, source.id));
        this.source = source;
    }
    @Override public FunnelBlock get() { return (FunnelBlock) source.get(); }
}
