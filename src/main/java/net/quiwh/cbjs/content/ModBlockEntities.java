package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.MaterialCogwheelVisual;
import net.quiwh.cbjs.client.MaterialCogwheelRenderer;
import net.quiwh.cbjs.client.MaterialEncasedCogwheelVisual;
import net.quiwh.cbjs.client.ModPartialModels;
import net.quiwh.cbjs.drives.DriveCogwheelBlockEntity;

import java.util.function.Supplier;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;

public final class ModBlockEntities {
    private static final BlockEntityBuilder<BracketedKineticBlockEntity, CreateRegistrate> COGWHEEL_BUILDER = CBJS.REGISTRATE
            .<BracketedKineticBlockEntity>blockEntity("cogwheel", DriveCogwheelBlockEntity::new)
            .visual(() -> {
                ModPartialModels.init();
                return MaterialCogwheelVisual::new;
            }, false)
            .renderer(() -> MaterialCogwheelRenderer::new)
            ;

    private static final BlockEntityBuilder<SimpleKineticBlockEntity, CreateRegistrate> ENCASED_BUILDER = CBJS.REGISTRATE
            .blockEntity("encased_cogwheel", SimpleKineticBlockEntity::new)
            .visual(() -> {
                ModPartialModels.init();
                return MaterialEncasedCogwheelVisual::new;
            }, false)
            .renderer(() -> MaterialCogwheelRenderer::new)
            ;

    public static final BlockEntityEntry<BracketedKineticBlockEntity> COGWHEEL = COGWHEEL_BUILDER.register();
    public static final BlockEntityEntry<SimpleKineticBlockEntity> ENCASED_COGWHEEL = ENCASED_BUILDER.register();

    public static void addCogwheelValidBlock(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(COGWHEEL::get, block);
    }

    public static void addEncasedValidBlock(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(ENCASED_COGWHEEL::get, block);
    }

    public static void register() {
    }

    private ModBlockEntities() {
    }
}
