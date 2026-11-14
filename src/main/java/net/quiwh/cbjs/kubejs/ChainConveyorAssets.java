package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Preserve native OBJ loader settings and material slots alongside script-selected geometry. */
final class ChainConveyorAssets {
    private static final String NATIVE = "create:block/chain_conveyor/";
    private static final Map<String, String> ALIASES = Map.of("casing", "conveyor_casing", "guard", "conveyor_port", "wheel", "bullwheel");
    private static final Set<String> ROLES = Set.of("all", "north", "casing", "guard", "wheel");
    static void generate(ChainConveyorBuilder b, KubeAssetGenerator g) {
        String root = "block/" + b.id.getPath() + "/";
        JsonObject body = b.modelOverrides.customPrimary() ? b.modelOverrides.primaryModel(NATIVE + "block") : nativeModel("block");
        textures(b, body, true);
        GearAssets.put(g, b.id, "models", root + "block", body);
        for (String role : new String[]{"shaft", "wheel", "guard"}) {
            JsonObject part = b.modelOverrides.has(role) ? b.modelOverrides.partModel(role, NATIVE + role) : nativeModel(role);
            textures(b, part, false);
            GearAssets.put(g, b.id, "models", root + role, part);
        }
        JsonObject variants = new JsonObject(), variant = new JsonObject(), states = new JsonObject();
        variant.addProperty("model", b.id.getNamespace() + ":" + root + "block"); variants.add("", variant); states.add("variants", variants);
        GearAssets.put(g, b.id, "blockstates", b.id.getPath(), b.blockstateJson == null ? states : b.blockstateJson);
        if (b.itemBuilder != null) {
            boolean primary = b.modelOverrides.usesPrimaryItem();
            JsonObject item = primary ? GearAssets.parent(b.model) : nativeModel("item");
            textures(b, item, false);
            item = GearAssets.itemOverrides(item, CBJSAssets.itemModel(b.itemBuilder), primary ? null : CBJSAssets.parent(b.itemBuilder), CBJSAssets.itemTextures(b.itemBuilder));
            GearAssets.put(g, b.id, "models/item", b.id.getPath(), item);
        }
    }
    private static JsonObject nativeModel(String role) { return ScriptModelResources.load(ResourceLocation.parse(NATIVE + role)).deepCopy(); }
    private static void textures(ChainConveyorBuilder b, JsonObject model, boolean body) {
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        String all = b.textures.containsKey("all") ? b.textures.get("all")
                : b.textures.containsKey("north") ? b.textures.get("north") : null;
        if (all != null) for (String slot : new String[]{"axis", "axis_top", "conveyor_casing", "conveyor_port", "bullwheel", "particle", "0"}) slots.addProperty(slot, all);
        for (var texture : b.textures.entrySet()) {
            if (ROLES.contains(texture.getKey())) {
                String alias = ALIASES.get(texture.getKey());
                if (alias != null) slots.addProperty(alias, texture.getValue());
            } else slots.addProperty(texture.getKey(), texture.getValue());
        }
        if (body) {
            if (b.textures.containsKey("casing")) slots.addProperty("0", b.textures.get("casing"));
            else if (b.textures.containsKey("conveyor_casing")) slots.addProperty("0", b.textures.get("conveyor_casing"));
        }
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
    }
    private ChainConveyorAssets() { }
}
