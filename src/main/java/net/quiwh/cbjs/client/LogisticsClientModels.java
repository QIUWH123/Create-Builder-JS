package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kubejs.*;

/** Eagerly declares generated material flaps for both buffered and Flywheel render paths. */
public final class LogisticsClientModels {
    private static final Map<net.minecraft.resources.ResourceLocation, Map<PartialModel, PartialModel>> MODELS = new ConcurrentHashMap<>();
    public static void declare(FunnelBuilder builder) {
        MODELS.put(builder.id, Map.of(AllPartialModels.FUNNEL_FLAP, PartialModel.of(NativeMachineAssets.partModelId(builder.id, "flap")),
                AllPartialModels.BELT_FUNNEL_FLAP, PartialModel.of(NativeMachineAssets.partModelId(builder.id, "belt_flap"))));
    }
    public static void declare(TunnelBuilder builder) {
        MODELS.put(builder.id, Map.of(AllPartialModels.BELT_TUNNEL_FLAP, PartialModel.of(NativeMachineAssets.partModelId(builder.id, "flap"))));
    }
    public static void registerConnectedTextures(TunnelBuilder builder) {
        var shift = com.simibubi.create.foundation.block.connected.CTSpriteShifter.getCT(
                com.simibubi.create.foundation.block.connected.AllCTTypes.VERTICAL,
                net.minecraft.resources.ResourceLocation.parse(builder.topTexture(false)),
                net.minecraft.resources.ResourceLocation.parse(builder.topTexture(true)));
        var behavior = new com.simibubi.create.content.logistics.tunnel.BrassTunnelCTBehaviour() {
            @Override public com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry getShift(BlockState state,
                    net.minecraft.core.Direction face, net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
                return face == net.minecraft.core.Direction.UP ? shift : null;
            }
        };
        com.simibubi.create.foundation.data.CreateRegistrate.blockModel(() -> model ->
                new com.simibubi.create.foundation.block.connected.CTModel(model, behavior)).accept(builder.get());
    }
    public static PartialModel select(BlockState state, PartialModel fallback) {
        NativeMachineBuilder builder = state.getBlock() instanceof ScriptedFunnelBlock b ? b.sourceBuilder()
                : state.getBlock() instanceof ScriptedBeltFunnelBlock b ? b.sourceBuilder().funnel
                : state.getBlock() instanceof ScriptedTunnelBlock b ? b.sourceBuilder()
                : state.getBlock() instanceof ScriptedBrassTunnelBlock b ? b.sourceBuilder() : null;
        var parts = builder == null ? null : MODELS.get(builder.id);
        return parts == null ? fallback : parts.getOrDefault(fallback, fallback);
    }
    private LogisticsClientModels() { }
}
