package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.Set;

/** References installed Create geometry; each visible native texture slot can be replaced. */
final class TransmissionAssets {
    private static final Set<String> ROLES = Set.of("side", "powered", "top", "gearbox", "frame", "brass", "casing", "bracket", "axis", "axis_top", "all", "north");
    static void generate(TransmissionBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath() + "/";
        boolean controller = builder.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER;
        GearAssets.put(generator, builder.id, "models", root + "block", body(builder, false));
        if (!controller) GearAssets.put(generator, builder.id, "models", root + "block_powered", body(builder, true));
        String shaft = controller ? "shaft" : "shaft_half";
        GearAssets.put(generator, builder.id, "models", root + shaft, shaft(builder, shaft));
        if (controller) GearAssets.put(generator, builder.id, "models", root + "bracket", bracket(builder));
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), builder.blockstateJson == null ? blockstate(builder) : builder.blockstateJson);
        if (builder.itemBuilder != null) GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item(builder));
    }
    private static String nativeRoot(TransmissionBuilder builder) { return "create:block/" + builder.kind.path + "/"; }
    private static JsonObject body(TransmissionBuilder builder, boolean powered) {
        boolean explicit = powered && builder.modelOverrides.has("powered");
        JsonObject model = explicit ? builder.modelOverrides.partModel("powered", nativeRoot(builder) + "block_powered")
                : builder.modelOverrides.primaryModel(nativeRoot(builder) + (powered ? "block_powered" : "block"));
        boolean nativeModel = !explicit && !builder.modelOverrides.customPrimary();
        JsonObject slots = slots(model);
        if (builder.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER) {
            put(builder, slots, "0", "side", "create:block/rotation_speed_controller", nativeModel);
            put(builder, slots, "4", "gearbox", "create:block/brass_gearbox", nativeModel);
            put(builder, slots, "5", "top", "create:block/rotation_speed_controller_top", nativeModel);
            put(builder, slots, "6", "brass", "create:block/brass_block", nativeModel);
            put(builder, slots, "7", "frame", "create:block/funnel/brass_funnel_neutral", nativeModel);
            put(builder, slots, "particle", "brass", "create:block/brass_block", nativeModel);
        } else {
            String role = powered && builder.textures.has("powered") ? "powered" : "side";
            put(builder, slots, "0", role, "create:block/" + builder.kind.path + (powered ? "_on" : "_off"), nativeModel);
            put(builder, slots, "particle", role, "create:block/" + builder.kind.path + (powered ? "_on" : "_off"), nativeModel);
            put(builder, slots, "1", "gearbox", "create:block/gearbox", nativeModel);
            put(builder, slots, "2", "frame", "create:block/funnel/andesite_funnel_frame", nativeModel);
        }
        return finish(builder, model, slots);
    }
    private static JsonObject shaft(TransmissionBuilder builder, String role) {
        JsonObject model = builder.modelOverrides.partModel(role, "create:block/" + role);
        JsonObject slots = slots(model); boolean nativeModel = !builder.modelOverrides.has(role);
        put(builder, slots, "0", "axis", "create:block/axis", nativeModel);
        put(builder, slots, "1", "axis_top", "create:block/axis_top", nativeModel);
        put(builder, slots, "particle", "axis", "create:block/axis", nativeModel);
        return finish(builder, model, slots);
    }
    private static JsonObject bracket(TransmissionBuilder builder) {
        JsonObject model = builder.modelOverrides.partModel("bracket", nativeRoot(builder) + "bracket");
        JsonObject slots = slots(model); boolean nativeModel = !builder.modelOverrides.has("bracket");
        put(builder, slots, "2", "casing", "create:block/brass_casing", nativeModel);
        put(builder, slots, "4", "brass", "create:block/brass_block", nativeModel);
        put(builder, slots, "5", "bracket", "create:block/rotation_speed_controller_bracket", nativeModel);
        put(builder, slots, "7", "frame", "create:block/funnel/brass_funnel_neutral", nativeModel);
        put(builder, slots, "particle", "brass", "create:block/brass_block", nativeModel);
        return finish(builder, model, slots);
    }
    private static JsonObject item(TransmissionBuilder builder) {
        boolean sharedPrimary = builder.modelOverrides.usesPrimaryItem();
        JsonObject model = GearAssets.parent(sharedPrimary ? builder.model : nativeRoot(builder) + "item");
        JsonObject slots = slots(model); boolean nativeModel = !sharedPrimary;
        if (builder.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER) {
            put(builder, slots, "0", "axis", "create:block/axis", nativeModel);
            put(builder, slots, "3", "axis_top", "create:block/axis_top", nativeModel);
            put(builder, slots, "4", "gearbox", "create:block/brass_gearbox", nativeModel);
            put(builder, slots, "7", "frame", "create:block/funnel/brass_funnel_neutral", nativeModel);
            put(builder, slots, "9", "brass", "create:block/brass_block", nativeModel);
            put(builder, slots, "1_0", "side", "create:block/rotation_speed_controller", nativeModel);
            put(builder, slots, "1_5", "top", "create:block/rotation_speed_controller_top", nativeModel);
        } else {
            put(builder, slots, "0", "side", "create:block/" + builder.kind.path + "_off", nativeModel);
            put(builder, slots, "1", "gearbox", "create:block/gearbox", nativeModel);
            put(builder, slots, "4", "frame", "create:block/funnel/andesite_funnel_frame", nativeModel);
            put(builder, slots, "1_0", "axis", "create:block/axis", nativeModel);
            put(builder, slots, "1_1", "axis_top", "create:block/axis_top", nativeModel);
        }
        put(builder, slots, "particle", "axis", "create:block/axis", nativeModel);
        finish(builder, model, slots);
        return GearAssets.itemOverrides(model, builder.itemBuilder.modelJson, sharedPrimary ? null : builder.itemBuilder.parentModel, builder.itemBuilder.textureJson);
    }
    private static JsonObject slots(JsonObject model) { return model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject(); }
    private static void put(TransmissionBuilder builder, JsonObject slots, String slot, String role, String fallback, boolean nativeModel) {
        if (builder.textures.has(role)) slots.add(slot, builder.textures.get(role));
        else if (builder.textures.has("all")) slots.add(slot, builder.textures.get("all"));
        else if (builder.textures.has("north")) slots.add(slot, builder.textures.get("north"));
        else if (nativeModel) slots.addProperty(slot, fallback);
    }
    private static JsonObject finish(TransmissionBuilder builder, JsonObject model, JsonObject slots) {
        for (var entry : builder.textures.entrySet()) if (!ROLES.contains(entry.getKey())) slots.add(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
        return model;
    }
    private static JsonObject blockstate(TransmissionBuilder builder) {
        JsonObject variants = new JsonObject();
        boolean controller = builder.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER;
        for (String axis : controller ? new String[]{"x", "z"} : new String[]{"x", "y", "z"})
            for (boolean powered : controller ? new boolean[]{false} : new boolean[]{false, true}) {
                JsonObject variant = new JsonObject();
                variant.addProperty("model", builder.id.getNamespace() + ":block/" + builder.id.getPath() + (powered ? "/block_powered" : "/block"));
                int x = controller || axis.equals("y") ? 0 : 90;
                int y = axis.equals("x") ? 90 : !controller && axis.equals("z") ? 180 : 0;
                if (x != 0) variant.addProperty("x", x);
                if (y != 0) variant.addProperty("y", y);
                variants.add("axis=" + axis + (controller ? "" : ",powered=" + powered), variant);
            }
        JsonObject result = new JsonObject(); result.add("variants", variants); return result;
    }
    private TransmissionAssets() { }
}
