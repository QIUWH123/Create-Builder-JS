package net.quiwh.cbjs.client;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllSpriteShifts;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import net.quiwh.cbjs.kubejs.BlazeBurnerBuilder;

/** Strongly held, eagerly declared partials shared by world and contraption render paths. */
public final class BlazeBurnerClientModels {
    private static final Map<ResourceLocation, Map<PartialModel, PartialModel>> MODELS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, SpriteShiftEntry[]> FLAMES = new ConcurrentHashMap<>();
    public static void declare(BlazeBurnerBuilder builder) {
        Map<PartialModel, PartialModel> parts = new HashMap<>();
        part(builder, parts, AllPartialModels.BLAZE_IDLE, "blaze");
        part(builder, parts, AllPartialModels.BLAZE_ACTIVE, "blaze_active");
        part(builder, parts, AllPartialModels.BLAZE_SUPER, "blaze_super");
        part(builder, parts, AllPartialModels.BLAZE_SUPER_ACTIVE, "blaze_super_active");
        part(builder, parts, AllPartialModels.BLAZE_INERT, "inert");
        part(builder, parts, AllPartialModels.BLAZE_BURNER_RODS, "rods");
        part(builder, parts, AllPartialModels.BLAZE_BURNER_RODS_2, "rods_2");
        part(builder, parts, AllPartialModels.BLAZE_BURNER_SUPER_RODS, "super_rods");
        part(builder, parts, AllPartialModels.BLAZE_BURNER_SUPER_RODS_2, "super_rods_2");
        part(builder, parts, AllPartialModels.BLAZE_BURNER_FLAME, "flame");
        part(builder, parts, AllPartialModels.BLAZE_GOGGLES, "goggles");
        part(builder, parts, AllPartialModels.BLAZE_GOGGLES_SMALL, "goggles_small");
        part(builder, parts, AllPartialModels.TRAIN_HAT, "train_hat");
        part(builder, parts, AllPartialModels.LOGISTICS_HAT, "logistics_hat");
        MODELS.put(builder.id, Map.copyOf(parts));
    }
    public static void register(BlazeBurnerBuilder builder) {
        var flame = ResourceLocation.parse(builder.texturePath("flame"));
        FLAMES.put(builder.id, new SpriteShiftEntry[] {
                SpriteShifter.get(flame, ResourceLocation.parse(builder.texturePath("flame_scroll"))),
                SpriteShifter.get(flame, ResourceLocation.parse(builder.texturePath("super_flame_scroll"))) });
    }
    private static void part(BlazeBurnerBuilder builder, Map<PartialModel, PartialModel> models, PartialModel nativeModel, String role) {
        models.put(nativeModel, PartialModel.of(ResourceLocation.fromNamespaceAndPath(builder.id.getNamespace(), "block/" + builder.id.getPath() + "/" + role)));
    }
    public static PartialModel select(BlockState state, PartialModel fallback) {
        var builder = BlazeBurnerFamilies.builder(state);
        var models = builder == null ? null : MODELS.get(builder.id);
        return models == null ? fallback : models.getOrDefault(fallback, fallback);
    }
    public static SpriteShiftEntry flame(BlockState state, SpriteShiftEntry fallback) {
        var builder = BlazeBurnerFamilies.builder(state);
        var shifts = builder == null ? null : FLAMES.get(builder.id);
        return shifts == null ? fallback : shifts[fallback == AllSpriteShifts.SUPER_BURNER_FLAME ? 1 : 0];
    }
    private BlazeBurnerClientModels() { }
}
