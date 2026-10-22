package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.Set;

/** Native chain-drive parents and orientations, with no copied geometry or connected textures. */
final class ChainDriveAssets {
    private static final String ROOT = "create:block/encased_chain_drive/";
    private static final String[] PARTS = {"single", "end_horizontal", "end_vertical", "middle_horizontal", "middle_vertical"};
    private static final Set<String> TEXTURE_ROLES = Set.of("side", "end", "middle", "gearbox", "axis", "axis_top", "all", "north");

    static void generate(ChainDriveBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath() + "/";
        for (String part : PARTS)
            GearAssets.put(generator, builder.id, "models", root + part, bodyModel(builder, part));
        GearAssets.put(generator, builder.id, "models", root + "shaft", shaftModel(builder));
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(), builder.blockstateJson == null
                ? blockstate(builder) : builder.blockstateJson);
        if (builder.itemBuilder != null)
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), itemModel(builder));
    }

    private static JsonObject bodyModel(ChainDriveBuilder builder, String part) {
        boolean customPart = builder.modelOverrides.has(part);
        JsonObject model = customPart ? builder.modelOverrides.partModel(part, ROOT + part)
                : builder.modelOverrides.primaryModel(ROOT + part);
        boolean nativeModel = !customPart && !builder.modelOverrides.customPrimary();
        JsonObject slots = slots(model);
        put(builder, slots, "side", "side", "create:block/encased_chain_drive", nativeModel);
        put(builder, slots, "particle", "side", "create:block/encased_chain_drive", nativeModel);
        if (part.startsWith("end"))
            put(builder, slots, "2", "end", "create:block/encased_chain_drive_end", nativeModel);
        else if (part.startsWith("middle"))
            put(builder, slots, "1", "middle", "create:block/encased_chain_drive_middle", nativeModel);
        else put(builder, slots, "1", "gearbox", "create:block/gearbox", nativeModel);
        finishTextures(builder, model, slots);
        return model;
    }

    static JsonObject shaftModel(ChainDriveBuilder builder) {
        boolean nativeModel = !builder.modelOverrides.has("shaft");
        JsonObject model = builder.modelOverrides.partModel("shaft", "create:block/shaft");
        JsonObject slots = slots(model);
        put(builder, slots, "0", "axis", "create:block/axis", nativeModel);
        put(builder, slots, "1", "axis_top", "create:block/axis_top", nativeModel);
        put(builder, slots, "particle", "axis", "create:block/axis", nativeModel);
        finishTextures(builder, model, slots);
        return model;
    }

    private static JsonObject itemModel(ChainDriveBuilder builder) {
        boolean sharedPrimary = builder.modelOverrides.usesPrimaryItem();
        JsonObject model = GearAssets.parent(sharedPrimary ? builder.model : ROOT + "item");
        JsonObject slots = slots(model);
        boolean nativeModel = !sharedPrimary;
        put(builder, slots, "side", "side", "create:block/encased_chain_drive", nativeModel);
        put(builder, slots, "particle", "side", "create:block/encased_chain_drive", nativeModel);
        put(builder, slots, "1", "gearbox", "create:block/gearbox", nativeModel);
        put(builder, slots, "1_0", "axis", "create:block/axis", nativeModel);
        put(builder, slots, "1_1", "axis_top", "create:block/axis_top", nativeModel);
        finishTextures(builder, model, slots);
        return GearAssets.itemOverrides(model, builder.itemBuilder.modelJson,
                sharedPrimary ? null : builder.itemBuilder.parentModel, builder.itemBuilder.textureJson);
    }

    private static JsonObject slots(JsonObject model) {
        return model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
    }

    private static void put(ChainDriveBuilder builder, JsonObject slots, String slot,
                            String role, String fallback, boolean nativeModel) {
        if (builder.textures.has(role)) slots.add(slot, builder.textures.get(role));
        else if (builder.textures.has("all")) slots.add(slot, builder.textures.get("all"));
        else if (builder.textures.has("north")) slots.add(slot, builder.textures.get("north"));
        else if (nativeModel) slots.addProperty(slot, fallback);
    }

    private static void finishTextures(ChainDriveBuilder builder, JsonObject model, JsonObject slots) {
        for (var entry : builder.textures.entrySet())
            if (!TEXTURE_ROLES.contains(entry.getKey())) slots.add(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
    }

    /** Match Create's ChainDriveGenerator for every axis, row direction and row section. */
    private static JsonObject blockstate(ChainDriveBuilder builder) {
        JsonObject variants = new JsonObject();
        for (String axis : new String[]{"x", "y", "z"})
            for (boolean first : new boolean[]{false, true})
                for (String part : new String[]{"start", "middle", "end", "none"}) {
                    String suffix = part.equals("none") ? "single"
                            : (part.equals("middle") ? "middle" : "end") + (axis.equals("y") ? "_vertical" : "_horizontal");
                    JsonObject variant = new JsonObject();
                    variant.addProperty("model", builder.id.getNamespace() + ":block/" + builder.id.getPath() + "/" + suffix);
                    int x = xRotation(axis, first, part);
                    int y = yRotation(axis, first, part);
                    if (x != 0) variant.addProperty("x", x);
                    if (y != 0) variant.addProperty("y", y);
                    variants.add("axis=" + axis + ",axis_along_first=" + first + ",part=" + part, variant);
                }
        JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }

    private static int xRotation(String axis, boolean first, String part) {
        if (part.equals("none")) return axis.equals("y") ? 90 : 0;
        if (axis.equals("x")) return (first ? 90 : 0) + (part.equals("start") ? 180 : 0);
        if (axis.equals("z")) return first ? 0 : part.equals("start") ? 270 : 90;
        return 0;
    }

    private static int yRotation(String axis, boolean first, String part) {
        if (part.equals("none")) return axis.equals("x") ? 90 : 0;
        if (axis.equals("z")) return first && part.equals("end") ? 270 : 90;
        boolean flip = part.equals("end") && !first || part.equals("start") && first;
        return axis.equals("y") ? (first ? 90 : 0) + (flip ? 180 : 0) : 0;
    }

    private ChainDriveAssets() { }
}
