package net.quiwh.cbjs.content;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.GlassPipeVisual;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.TransparentStraightPipeRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;

import java.util.function.Supplier;

/** Registers native Create fluid transport and glass rendering for script-defined pipe families. */
public final class ModFluidPipeBlockEntities {
    private static final BlockEntityBuilder<FluidPipeBlockEntity, CreateRegistrate> PIPE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_fluid_pipe", FluidPipeBlockEntity::new);
    private static final BlockEntityBuilder<StraightPipeBlockEntity, CreateRegistrate> GLASS_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_glass_fluid_pipe", StraightPipeBlockEntity::new)
            .visual(() -> GlassPipeVisual::new, false)
            .renderer(() -> TransparentStraightPipeRenderer::new);

    public static final BlockEntityEntry<FluidPipeBlockEntity> FLUID_PIPE = PIPE_BUILDER.register();
    public static final BlockEntityEntry<StraightPipeBlockEntity> GLASS_FLUID_PIPE = GLASS_BUILDER.register();

    public static void addPipe(Supplier<? extends Block> block) { PIPE_BUILDER.validBlock(block::get); }

    public static void addGlassPipe(Supplier<? extends Block> block) { GLASS_BUILDER.validBlock(block::get); }

    public static void register() { }

    private ModFluidPipeBlockEntities() { }
}
