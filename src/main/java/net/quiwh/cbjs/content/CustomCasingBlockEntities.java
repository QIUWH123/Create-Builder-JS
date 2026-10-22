package net.quiwh.cbjs.content;

import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;

import java.util.function.Supplier;

/** Adds script-generated casing variants to their block entities before registration. */
public final class CustomCasingBlockEntities {
    private static final BlockEntityBuilder<KineticBlockEntity, CreateRegistrate> SHAFT_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_encased_shaft", KineticBlockEntity::new)
            .visual(() -> net.quiwh.cbjs.client.ShaftClientModels.EncasedShaftVisual::new, false)
            .renderer(() -> net.quiwh.cbjs.client.ShaftClientModels.ShaftRenderer::new);

    private static final BlockEntityBuilder<SimpleKineticBlockEntity, CreateRegistrate> COGWHEEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_encased_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> EncasedCogVisual::small, false)
            .renderer(() -> EncasedCogRenderer::small);

    private static final BlockEntityBuilder<SimpleKineticBlockEntity, CreateRegistrate> LARGE_COGWHEEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_encased_large_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> EncasedCogVisual::large, false)
            .renderer(() -> EncasedCogRenderer::large);

    private static final BlockEntityBuilder<FluidPipeBlockEntity, CreateRegistrate> PIPE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_encased_fluid_pipe", FluidPipeBlockEntity::new);

    public static final BlockEntityEntry<KineticBlockEntity> ENCASED_SHAFT = SHAFT_BUILDER.register();
    public static final BlockEntityEntry<SimpleKineticBlockEntity> ENCASED_COGWHEEL = COGWHEEL_BUILDER.register();
    public static final BlockEntityEntry<SimpleKineticBlockEntity> ENCASED_LARGE_COGWHEEL = LARGE_COGWHEEL_BUILDER.register();
    public static final BlockEntityEntry<FluidPipeBlockEntity> ENCASED_FLUID_PIPE = PIPE_BUILDER.register();

    public static void addShaft(Supplier<? extends Block> block) {
        SHAFT_BUILDER.validBlock(block::get);
    }

    public static void addCogwheel(Supplier<? extends Block> block, boolean large) {
        (large ? LARGE_COGWHEEL_BUILDER : COGWHEEL_BUILDER).validBlock(block::get);
    }

    public static void addPipe(Supplier<? extends Block> block) {
        PIPE_BUILDER.validBlock(block::get);
    }

    public static void register() {
    }

    private CustomCasingBlockEntities() {
    }
}
