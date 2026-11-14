package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedSteamEngineBlock;
import net.quiwh.cbjs.content.ScriptedSteamWhistleBlock;
import net.quiwh.cbjs.kubejs.SteamEngineBuilder;
import net.quiwh.cbjs.kubejs.SteamWhistleBuilder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SteamClientModels {
    private static final Map<ResourceLocation, Map<String, PartialModel>> PARTS = new ConcurrentHashMap<>();
    public static void declare(SteamEngineBuilder b) { declare(b.id, new String[]{"piston", "linkage", "connector"}); }
    public static void declare(SteamWhistleBuilder b) { declare(b.id, new String[]{"mouth_small", "mouth_medium", "mouth_large"}); }
    private static void declare(ResourceLocation id, String[] names) {
        Map<String, PartialModel> parts = new java.util.HashMap<>();
        for (String name : names) parts.put(name, PartialModel.of(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/" + name)));
        PARTS.put(id, Map.copyOf(parts));
    }
    public static PartialModel select(PartialModel original, BlockState state) {
        ResourceLocation id = state.getBlock() instanceof ScriptedSteamEngineBlock engine ? engine.sourceBuilder().id
                : state.getBlock() instanceof ScriptedSteamWhistleBlock whistle ? whistle.sourceBuilder().id : null;
        if (id == null) return original;
        String part = original == AllPartialModels.ENGINE_PISTON ? "piston" : original == AllPartialModels.ENGINE_LINKAGE ? "linkage"
                : original == AllPartialModels.ENGINE_CONNECTOR ? "connector" : original == AllPartialModels.WHISTLE_MOUTH_SMALL ? "mouth_small"
                : original == AllPartialModels.WHISTLE_MOUTH_MEDIUM ? "mouth_medium" : original == AllPartialModels.WHISTLE_MOUTH_LARGE ? "mouth_large" : "";
        return PARTS.getOrDefault(id, Map.of()).getOrDefault(part, original);
    }
    public static final class PoweredShaftVisual extends SingleAxisRotatingVisual<PoweredShaftBlockEntity> {
        public PoweredShaftVisual(VisualizationContext context, PoweredShaftBlockEntity entity, float partialTick) {
            super(context, entity, partialTick, Models.partial(ShaftClientModels.model(entity.getBlockState())));
        }
    }
    private SteamClientModels() { }
}
