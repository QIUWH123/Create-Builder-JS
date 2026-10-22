package net.quiwh.cbjs.compat.bnb;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.MaterialCogwheelVisual;
import net.quiwh.cbjs.client.ModPartialModels;
import net.quiwh.cbjs.compat.bnb.client.BnbChainedCogwheelRenderer;

/** A distinct valid-block set avoids modifying BnB's registered entity type. */
public final class BnbChainBlockEntities {
    private static final BlockEntityBuilder<BnbChainedCogwheelBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("chained_cogwheel", BnbChainedCogwheelBlockEntity::new)
            .visual(() -> {
                ModPartialModels.init();
                return MaterialCogwheelVisual::new;
            }, true)
            .renderer(() -> BnbChainedCogwheelRenderer::new);

    public static final BlockEntityEntry<BnbChainedCogwheelBlockEntity> COGWHEEL_CHAIN = BUILDER.register();

    public static void add(Supplier<? extends Block> block) { BUILDER.validBlock(block::get); }
    public static void register() { }
    private BnbChainBlockEntities() { }
}
