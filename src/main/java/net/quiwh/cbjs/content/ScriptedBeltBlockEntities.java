package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.content.kinetics.belt.BeltVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import java.util.function.Supplier;

/** Shares Create's complete inventory, processing, casing and kinetic implementation. */
public final class ScriptedBeltBlockEntities {
    private static final BlockEntityBuilder<BeltBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_mechanical_belt", BeltBlockEntity::new)
            .visual(() -> BeltVisual::new, BeltBlockEntity::shouldRenderNormally)
            .renderer(() -> BeltRenderer::new);
    public static final BlockEntityEntry<BeltBlockEntity> BELT = BUILDER.register();
    public static void add(Supplier<? extends Block> block) { BUILDER.validBlock(block::get); }
    public static void register() { }
    private ScriptedBeltBlockEntities() { }
}
