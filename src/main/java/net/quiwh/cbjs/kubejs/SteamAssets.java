package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** Native geometry aliases for the static and moving steam components. */
final class SteamAssets {
    static final String[] SIZES = {"small", "medium", "large"};
    static final Set<String> WHISTLE_PARTS;
    static {
        var parts = new java.util.HashSet<String>();
        for (String size : SIZES) {
            parts.add("mouth_" + size);
            for (String mount : new String[]{"floor", "wall"}) {
                parts.add(size + "_" + mount); parts.add(size + "_" + mount + "_powered");
            }
            for (String shape : new String[]{"single", "double", "top_rim"}) parts.add("extension_" + size + "_" + shape);
        }
        WHISTLE_PARTS = Set.copyOf(parts);
    }
    static void engine(SteamEngineBuilder b, KubeAssetGenerator g) {
        model(b, b.modelOverrides, g, "block", "create:block/steam_engine/block", true);
        for (String part : new String[]{"piston", "linkage", "connector"})
            model(b, b.modelOverrides, g, part, "create:block/steam_engine/" + (part.equals("connector") ? "shaft_connector" : part), false);
        JsonObject variants = new JsonObject();
        String[] directions = {"north", "east", "south", "west"};
        for (String face : new String[]{"floor", "wall", "ceiling"}) for (int i = 0; i < 4; i++) {
            JsonObject variant = reference(b.id, "block");
            int x = face.equals("floor") ? 0 : face.equals("wall") ? 90 : 180;
            int y = (i * 90 + (face.equals("ceiling") ? 180 : 0)) % 360;
            if (x != 0) variant.addProperty("x", x); if (y != 0) variant.addProperty("y", y);
            variants.add("face=" + face + ",facing=" + directions[i], variant);
        }
        state(b, g, variants);
        item(b, b.modelOverrides, g, "create:block/steam_engine/item");
    }
    static void whistle(SteamWhistleBuilder b, KubeAssetGenerator g) {
        JsonObject variants = new JsonObject();
        String[] directions = {"south", "west", "north", "east"};
        for (String size : SIZES) {
            model(b, b.modelOverrides, g, "mouth_" + size, "create:block/steam_whistle/" + size + "_mouth", false);
            for (boolean wall : new boolean[]{false, true}) for (boolean powered : new boolean[]{false, true}) {
                String role = size + (wall ? "_wall" : "_floor") + (powered ? "_powered" : "");
                model(b, b.modelOverrides, g, role, "create:block/steam_whistle/block_" + role, true);
                for (int i = 0; i < 4; i++) {
                    JsonObject variant = reference(b.id, role); if (i != 0) variant.addProperty("y", i * 90);
                    variants.add("facing=" + directions[i] + ",size=" + size + ",wall=" + wall + ",powered=" + powered, variant);
                }
            }
        }
        state(b, g, variants); item(b, b.modelOverrides, g, "create:block/steam_whistle/item");
    }
    static void extension(SteamWhistleExtensionBuilder b, KubeAssetGenerator g) {
        JsonArray multipart = new JsonArray();
        for (String size : SIZES) for (String shape : new String[]{"single", "double", "top_rim"}) {
            String role = "extension_" + size + "_" + shape;
            model(b.whistle, b.whistle.modelOverrides, g, role, "create:block/steam_whistle/extension/" + size + "_" + shape, false);
            JsonObject part = new JsonObject(), when = new JsonObject();
            when.addProperty("size", size);
            when.addProperty("shape", shape.equals("top_rim") ? "double" : shape.equals("double") ? "double|double_connected" : "single");
            part.add("when", when); part.add("apply", reference(b.whistle.id, role)); multipart.add(part);
        }
        JsonObject state = new JsonObject(); state.add("multipart", multipart);
        GearAssets.put(g, b.id, "blockstates", b.id.getPath(), state);
    }
    private static JsonObject reference(ResourceLocation id, String role) {
        JsonObject ref = new JsonObject(); ref.addProperty("model", id.getNamespace() + ":block/" + id.getPath() + "/" + role); return ref;
    }
    private static void state(FluidDeviceBuilder b, KubeAssetGenerator g, JsonObject variants) {
        JsonObject state = new JsonObject(); state.add("variants", variants);
        GearAssets.put(g, b.id, "blockstates", b.id.getPath(), b.blockstateJson == null ? state : b.blockstateJson);
    }
    private static void model(FluidDeviceBuilder b, ModelOverrides overrides, KubeAssetGenerator g, String role, String source, boolean primary) {
        boolean custom = overrides.has(role) || primary && overrides.customPrimary();
        JsonObject model = overrides.has(role) ? overrides.partModel(role, source)
                : primary ? overrides.primaryModel(source) : overrides.partModel(role, source);
        textures(b, model, source, custom);
        GearAssets.put(g, b.id, "models", "block/" + b.id.getPath() + "/" + role, model);
    }
    private static void item(FluidDeviceBuilder b, ModelOverrides overrides, KubeAssetGenerator g, String source) {
        if (b.itemBuilder == null) return;
        boolean shared = overrides.usesPrimaryItem();
        JsonObject model = GearAssets.parent(shared ? b.model : source); textures(b, model, source, shared);
        GearAssets.put(g, b.id, "models/item", b.id.getPath(), GearAssets.itemOverrides(model,
                CBJSAssets.itemModel(b.itemBuilder), shared ? null : CBJSAssets.parent(b.itemBuilder), CBJSAssets.itemTextures(b.itemBuilder)));
    }
    private static void textures(FluidDeviceBuilder b, JsonObject model, String source, boolean custom) {
        JsonObject nativeSlots = GearAssets.resolved(ResourceLocation.parse(source)).getAsJsonObject("textures");
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        String all = b.textures.containsKey("all") ? b.textures.get("all") : b.allTexture;
        for (var e : nativeSlots.entrySet()) {
            String value = e.getValue().getAsString();
            String role = value.contains("cam_linkage") ? "linkage" : value.contains("redstone_plate_powered") ? "powered"
                    : value.contains("redstone_plate") ? "plate" : value.contains("whistle") ? "whistle"
                    : value.contains("engine") ? "engine" : "particle";
            if (b.textures.containsKey(role)) slots.addProperty(e.getKey(), b.textures.get(role));
            else if (all != null) slots.addProperty(e.getKey(), all);
            else if (!custom) slots.add(e.getKey(), e.getValue());
        }
        for (var e : b.textures.entrySet())
            if (!Set.of("linkage", "plate", "powered", "whistle", "engine", "all", "north", "south", "east", "west", "up", "down").contains(e.getKey())) slots.addProperty(e.getKey(), e.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
    }
    private SteamAssets() { }
}
