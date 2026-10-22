package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.ShaftClientModels;
import java.util.function.Supplier;

public final class ScriptedShaftBlockEntities {
    private static final BlockEntityBuilder<BracketedKineticBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_shaft", BracketedKineticBlockEntity::new)
            .visual(() -> ShaftClientModels.ShaftVisual::new, false)
            .renderer(() -> ShaftClientModels.ShaftRenderer::new);
    public static final BlockEntityEntry<BracketedKineticBlockEntity> SHAFT = BUILDER.register();
    public static void add(Supplier<? extends Block> block) { BUILDER.validBlock(block::get); }
    public static void register() { }
    private ScriptedShaftBlockEntities() { }
}
