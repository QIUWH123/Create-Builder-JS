package net.quiwh.cbjs.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.content.GearModelSet;
import net.quiwh.cbjs.content.ModCogwheels;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ModPartialModels {
    public record GearModels(PartialModel small, PartialModel large, PartialModel shaft, PartialModel shaftHalf) {
        public PartialModel rotating(boolean largeCogwheel) {
            return largeCogwheel ? large : small;
        }
    }

    private static final Map<GearModelSet, GearModels> MODELS = new ConcurrentHashMap<>();

    public static GearModels of(GearModelSet models) {
        return MODELS.computeIfAbsent(models, key -> new GearModels(PartialModel.of(key.small()),
                PartialModel.of(key.large()), PartialModel.of(key.shaft()), PartialModel.of(key.shaftHalf())));
    }

    public static void declareModels() {
        ModCogwheels.baseBlocks().forEach(block -> of(block.get().models()));
    }

    /** Compatibility hook used by the block-entity visual factories. */
    public static void init() {
        declareModels();
    }

    private ModPartialModels() {
    }
}
