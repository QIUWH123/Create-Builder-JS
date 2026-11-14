package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ScriptedChainConveyorBlock;
import net.quiwh.cbjs.kubejs.ChainConveyorBuilder;

/** Native renderer and Flywheel visual share these material-specific partials. */
public final class ChainConveyorClientModels {
    private record Parts(PartialModel shaft, PartialModel wheel, PartialModel guard) { }
    private static final Map<ResourceLocation, Parts> MODELS = new ConcurrentHashMap<>();
    public static void declare(ChainConveyorBuilder b) {
        MODELS.computeIfAbsent(b.id, id -> new Parts(part(id, "shaft"), part(id, "wheel"), part(id, "guard")));
    }
    private static PartialModel part(ResourceLocation id, String role) {
        return PartialModel.of(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/" + role));
    }
    public static PartialModel select(BlockState state, PartialModel original) {
        if (!(state.getBlock() instanceof ScriptedChainConveyorBlock block)) return original;
        Parts parts = MODELS.get(block.sourceBuilder().id);
        if (parts == null) return original;
        if (original == AllPartialModels.CHAIN_CONVEYOR_SHAFT) return parts.shaft;
        if (original == AllPartialModels.CHAIN_CONVEYOR_WHEEL) return parts.wheel;
        if (original == AllPartialModels.CHAIN_CONVEYOR_GUARD) return parts.guard;
        return original;
    }
    private ChainConveyorClientModels() { }
}
