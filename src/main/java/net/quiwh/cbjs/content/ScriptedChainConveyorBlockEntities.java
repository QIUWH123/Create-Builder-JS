package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.chainConveyor.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kubejs.ChainConveyorBuilder;

/** Native entity class preserves packages, ports, routing, riding and material-edge mixins. */
public final class ScriptedChainConveyorBlockEntities {
    private static final BlockEntityBuilder<ChainConveyorBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_chain_conveyor", ChainConveyorBlockEntity::new)
            .visual(() -> ChainConveyorVisual::new, false)
            .renderer(() -> ChainConveyorRenderer::new);
    public static final BlockEntityEntry<ChainConveyorBlockEntity> CONVEYOR = BUILDER.register();
    public static void add(ChainConveyorBuilder builder) {
        ScriptedBlockEntityValidity.add(CONVEYOR::get, builder::get);
    }
    public static void register() { }
    private ScriptedChainConveyorBlockEntities() { }
}
