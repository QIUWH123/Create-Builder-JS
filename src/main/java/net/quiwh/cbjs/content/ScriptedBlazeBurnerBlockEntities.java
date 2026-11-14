package net.quiwh.cbjs.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerVisual;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import java.util.function.Supplier;

/** A shared native burner entity type, valid only for the script-registered burner blocks. */
public final class ScriptedBlazeBurnerBlockEntities {
    private static final BlockEntityBuilder<ScriptedBlazeBurnerBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_blaze_burner", ScriptedBlazeBurnerBlockEntity::new)
            .visual(() -> BlazeBurnerVisual::new, false)
            .renderer(() -> BlazeBurnerRenderer::new);
    public static final BlockEntityEntry<ScriptedBlazeBurnerBlockEntity> BURNER = BUILDER.register();
    public static void add(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(BURNER::get, block);
    }
    public static void register() { }
    private ScriptedBlazeBurnerBlockEntities() { }
}
