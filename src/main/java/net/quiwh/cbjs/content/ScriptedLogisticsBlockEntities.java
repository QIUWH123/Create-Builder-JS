package net.quiwh.cbjs.content;

import com.simibubi.create.content.logistics.chute.*;
import com.simibubi.create.content.logistics.funnel.*;
import com.simibubi.create.content.logistics.tunnel.*;
import com.simibubi.create.content.processing.basin.*;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import java.util.function.Supplier;

/** Separate valid-block sets; every entity retains Create's native runtime implementation. */
public final class ScriptedLogisticsBlockEntities {
    private static final BlockEntityBuilder<ChuteBlockEntity, CreateRegistrate> CHUTE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_chute", ChuteBlockEntity::new)
            .renderer(() -> ChuteRenderer::new);
    public static final BlockEntityEntry<ChuteBlockEntity> CHUTE = CHUTE_BUILDER.register();
    private static final BlockEntityBuilder<SmartChuteBlockEntity, CreateRegistrate> SMART_CHUTE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_smart_chute", SmartChuteBlockEntity::new)
            .renderer(() -> SmartChuteRenderer::new);
    public static final BlockEntityEntry<SmartChuteBlockEntity> SMART_CHUTE = SMART_CHUTE_BUILDER.register();
    private static final BlockEntityBuilder<FunnelBlockEntity, CreateRegistrate> FUNNEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_funnel", FunnelBlockEntity::new).visual(() -> FunnelVisual::new)
            .renderer(() -> FunnelRenderer::new);
    public static final BlockEntityEntry<FunnelBlockEntity> FUNNEL = FUNNEL_BUILDER.register();
    private static final BlockEntityBuilder<BeltTunnelBlockEntity, CreateRegistrate> TUNNEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_andesite_tunnel", BeltTunnelBlockEntity::new).visual(() -> BeltTunnelVisual::new)
            .renderer(() -> BeltTunnelRenderer::new);
    public static final BlockEntityEntry<BeltTunnelBlockEntity> TUNNEL = TUNNEL_BUILDER.register();
    private static final BlockEntityBuilder<BrassTunnelBlockEntity, CreateRegistrate> BRASS_TUNNEL_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_brass_tunnel", BrassTunnelBlockEntity::new).visual(() -> BeltTunnelVisual::new)
            .renderer(() -> BeltTunnelRenderer::new);
    public static final BlockEntityEntry<BrassTunnelBlockEntity> BRASS_TUNNEL = BRASS_TUNNEL_BUILDER.register();
    private static final BlockEntityBuilder<BasinBlockEntity, CreateRegistrate> BASIN_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_basin", BasinBlockEntity::new)
            .renderer(() -> BasinRenderer::new);
    public static final BlockEntityEntry<BasinBlockEntity> BASIN = BASIN_BUILDER.register();
    public static void addChute(Supplier<? extends Block> block, boolean smart) {
        if (smart) ScriptedBlockEntityValidity.add(SMART_CHUTE::get, block);
        else ScriptedBlockEntityValidity.add(CHUTE::get, block);
    }
    public static void addFunnel(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(FUNNEL::get, block);
    }
    public static void addTunnel(Supplier<? extends Block> block, boolean brass) {
        if (brass) ScriptedBlockEntityValidity.add(BRASS_TUNNEL::get, block);
        else ScriptedBlockEntityValidity.add(TUNNEL::get, block);
    }
    public static void addBasin(Supplier<? extends Block> block) {
        ScriptedBlockEntityValidity.add(BASIN::get, block);
    }
    public static void register() { }
    private ScriptedLogisticsBlockEntities() { }
}
