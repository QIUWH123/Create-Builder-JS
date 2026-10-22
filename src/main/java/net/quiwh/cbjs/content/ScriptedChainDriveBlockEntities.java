package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.ChainDriveClientModels;
import net.quiwh.cbjs.kubejs.ChainDriveBuilder;

/** Shares native chain-drive kinetics, with a valid block set owned by scripted drives. */
public final class ScriptedChainDriveBlockEntities {
    private static final BlockEntityBuilder<KineticBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_encased_chain_drive", KineticBlockEntity::new)
            .visual(() -> ChainDriveClientModels.ChainDriveVisual::new, false)
            .renderer(() -> ChainDriveClientModels.ChainDriveRenderer::new);
    public static final BlockEntityEntry<KineticBlockEntity> CHAIN_DRIVE = BUILDER.register();

    public static void add(ChainDriveBuilder builder) { BUILDER.validBlock(builder::get); }
    public static void register() { }
    private ScriptedChainDriveBlockEntities() { }
}
