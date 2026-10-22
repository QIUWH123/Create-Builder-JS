package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import com.simibubi.create.content.kinetics.gearbox.GearboxVisual;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.DirectionalGearboxRenderer;
import net.quiwh.cbjs.client.DirectionalGearboxVisual;
import net.quiwh.cbjs.kubejs.GearboxBuilder;

/** Native gearbox entities preserve direction-dependent rotation in both render modes. */
public final class ScriptedGearboxBlockEntities {
    private static final BlockEntityBuilder<GearboxBlockEntity, CreateRegistrate> BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_gearbox", GearboxBlockEntity::new)
            .visual(() -> GearboxVisual::new, false)
            .renderer(() -> GearboxRenderer::new);
    public static final BlockEntityEntry<GearboxBlockEntity> GEARBOX = BUILDER.register();
    // Keep the existing six-way ID so saved scripted gearboxes retain their kinetics.
    private static final BlockEntityBuilder<DirectionalGearboxBlockEntity, CreateRegistrate> SIX_WAY_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_six_way_gearbox", DirectionalGearboxBlockEntity::new)
            .visual(() -> DirectionalGearboxVisual::new, false)
            .renderer(() -> DirectionalGearboxRenderer::new);
    public static final BlockEntityEntry<DirectionalGearboxBlockEntity> SIX_WAY_GEARBOX = SIX_WAY_BUILDER.register();
    public static void add(GearboxBuilder block) {
        switch (block.kind) {
            case SIX_WAY -> SIX_WAY_BUILDER.validBlock(block::get);
            default -> BUILDER.validBlock(block::get);
        }
    }
    public static void register() { }
    private ScriptedGearboxBlockEntities() { }
}
