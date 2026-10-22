package net.quiwh.cbjs.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.quiwh.cbjs.content.GearModelSet;
import net.quiwh.cbjs.content.ModCogwheels;

import java.util.HashMap;
import java.util.Map;

public final class ModPartialModels {
    public record GearModels(PartialModel small, PartialModel large, PartialModel shaft, PartialModel shaftHalf,
                             PartialModel complete) {
        public PartialModel rotating(boolean largeCogwheel, boolean encased) {
            return complete != null && !encased ? complete : largeCogwheel ? large : small;
        }
    }

    private static final Map<GearModelSet, GearModels> MODELS = new HashMap<>();

    public static GearModels of(GearModelSet models) {
        return MODELS.computeIfAbsent(models, key -> new GearModels(PartialModel.of(key.small()),
                PartialModel.of(key.large()), PartialModel.of(key.shaft()), PartialModel.of(key.shaftHalf()),
                key.complete() == null ? null : PartialModel.of(key.complete())));
    }

    public static void init() {
        // Resolve scripted metadata after block registration, before client model baking.
        ModCogwheels.baseBlocks().forEach(block -> of(block.get().models()));
    }

    private ModPartialModels() {
    }
}
