package net.quiwh.cbjs.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.kubejs.GearboxBuilder;
import dev.latvian.mods.kubejs.core.BlockKJS;

/** Native gearbox animation uses one stable, shared half-shaft partial per scripted material. */
public final class GearboxClientModels {
    private static final Map<ResourceLocation, PartialModel> SHAFTS = new ConcurrentHashMap<>();

    public static ResourceLocation shaftLocation(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath() + "/shaft_half");
    }
    public static void register(GearboxBuilder builder) {
        if (builder.modelOverrides.has("shaft"))
            SHAFTS.computeIfAbsent(builder.id, id -> PartialModel.of(shaftLocation(id)));
    }
    public static PartialModel shaft(BlockState state, PartialModel fallback) {
        if (!(state.getBlock() instanceof BlockKJS block) || !(block.kjs$getBlockBuilder() instanceof GearboxBuilder builder))
            return fallback;
        return SHAFTS.getOrDefault(builder.id, fallback);
    }

    private GearboxClientModels() { }
}
