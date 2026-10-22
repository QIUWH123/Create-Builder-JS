package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;

/** Device aliases inherit the installed Create version's geometry and placement rules. */
final class FluidDeviceAssets {
    private FluidDeviceAssets() { }

    static void generate(PumpBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath();
        JsonObject body = builder.modelOverrides.primaryModel("create:block/mechanical_pump/block");
        pumpBodyTextures(body, builder, customStationary(builder));
        GearAssets.put(generator, builder.id, "models", root + "/block", body);

        JsonObject cog = builder.modelOverrides.partModel("cog", "create:block/mechanical_pump/cog");
        if (!builder.modelOverrides.has("cog") || explicit(builder, "cog", "pump")) {
            textures(cog).addProperty("2", builder.cogTexture());
            textures(cog).addProperty("particle", builder.cogTexture());
        }
        rawTextures(cog, builder);
        GearAssets.put(generator, builder.id, "models", root + "/cog", cog);

        JsonObject item = pumpItem(builder.bodyTexture(), builder.cogTexture());
        rawTextures(item, builder);
        boolean primaryItem = builder.modelOverrides.usesPrimaryItem();
        if (primaryItem) {
            item = GearAssets.parent(builder.model);
            pumpBodyTextures(item, builder, true);
        }
        finish(builder, generator, pumpState(builder.id), item, !primaryItem);
    }

    static void generate(SmartFluidPipeBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath();
        JsonObject body = builder.modelOverrides.primaryModel("create:block/smart_fluid_pipe/block");
        smartTextures(body, builder, false);
        GearAssets.put(generator, builder.id, "models", root + "/block", body);
        JsonObject item = GearAssets.parent("create:block/smart_fluid_pipe/item");
        smartTextures(item, builder, true);
        boolean primaryItem = builder.modelOverrides.usesPrimaryItem();
        if (primaryItem) {
            item = GearAssets.parent(builder.model);
            smartTextures(item, builder, false);
        }
        finish(builder, generator, smartState(builder.id), item, !primaryItem);
    }

    static void generate(FluidValveBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath();
        for (String direction : new String[]{"horizontal", "vertical"})
            for (String state : new String[]{"closed", "open"}) {
                String name = "block_" + direction + "_" + state;
                JsonObject body = stationary(builder, "create:block/fluid_valve/" + name);
                if (!customStationary(builder) || explicit(builder, "fluid_valve"))
                    valveBodyTextures(body, builder);
                pipeAttachmentTexture(body, builder);
                rawTextures(body, builder);
                GearAssets.put(generator, builder.id, "models", root + "/" + name, body);
            }

        JsonObject pointer = GearAssets.parent("create:block/fluid_valve/pointer");
        valveBodyTextures(pointer, builder);
        pipeAttachmentTexture(pointer, builder);
        rawTextures(pointer, builder);
        GearAssets.put(generator, builder.id, "models", root + "/pointer", pointer);

        JsonObject item = GearAssets.parent("create:block/fluid_valve/item");
        valveBodyTextures(item, builder);
        pipeAttachmentTexture(item, builder);
        rawTextures(item, builder);
        finish(builder, generator, valveState(builder.id), item);
    }

    static JsonObject pumpState(ResourceLocation id) {
        return nativeState(id, "mechanical_pump");
    }

    static JsonObject smartState(ResourceLocation id) {
        return nativeState(id, "smart_fluid_pipe");
    }

    static JsonObject valveState(ResourceLocation id) {
        JsonObject state = ScriptModelResources.loadBlockstate(ResourceLocation.parse("create:fluid_valve"));
        for (var entry : state.getAsJsonObject("variants").entrySet()) {
            JsonObject variant = entry.getValue().getAsJsonObject();
            String model = variant.get("model").getAsString();
            variant.addProperty("model", id.getNamespace() + ":block/" + id.getPath()
                    + model.substring(model.lastIndexOf('/')));
        }
        return state;
    }

    private static JsonObject nativeState(ResourceLocation id, String nativeId) {
        JsonObject state = ScriptModelResources.loadBlockstate(ResourceLocation.parse("create:" + nativeId));
        for (var entry : state.getAsJsonObject("variants").entrySet())
            entry.getValue().getAsJsonObject().addProperty("model", id.getNamespace() + ":block/" + id.getPath() + "/block");
        return state;
    }

    private static JsonObject stationary(BlockBuilder builder, String nativeParent) {
        if (builder.modelJson != null) return builder.modelJson.deepCopy();
        return GearAssets.parent(builder.model != null && !builder.model.isEmpty() ? builder.model : nativeParent);
    }

    private static boolean customStationary(BlockBuilder builder) {
        return builder.modelJson != null || builder.model != null && !builder.model.isEmpty();
    }

    private static boolean explicit(FluidDeviceBuilder builder, String... roles) {
        if (builder.allTexture != null) return true;
        for (String role : roles) if (builder.textures.has(role)) return true;
        return false;
    }

    private static void pumpBodyTextures(JsonObject model, PumpBuilder builder, boolean customModel) {
        if (!customModel || explicit(builder, "pump")) {
            textures(model).addProperty("4", builder.bodyTexture());
            textures(model).addProperty("particle", builder.bodyTexture());
        }
        if (!customModel || explicit(builder, "pipe"))
            textures(model).addProperty("cbjs_pipe", builder.pipeTexture());
        rawTextures(model, builder);
    }

    static JsonObject pumpItem(String bodyTexture, String cogTexture) {
        JsonObject item;
        if (bodyTexture.equals(cogTexture)) item = GearAssets.parent("create:block/mechanical_pump/item");
        else {
            // Create's inventory mesh merges both materials into #4. Keep its native mesh and
            // bind only the cog, which lies between the two end housings, to the second atlas.
            item = ScriptModelResources.load(ResourceLocation.parse("create:block/mechanical_pump/item"));
            for (JsonElement element : item.getAsJsonArray("elements")) {
                JsonObject cube = element.getAsJsonObject();
                double fromX = cube.getAsJsonArray("from").get(0).getAsDouble();
                double toX = cube.getAsJsonArray("to").get(0).getAsDouble();
                if (fromX <= 0 || toX >= 16) continue;
                for (var face : cube.getAsJsonObject("faces").entrySet())
                    face.getValue().getAsJsonObject().addProperty("texture", "#2");
            }
            textures(item).addProperty("2", cogTexture);
        }
        textures(item).addProperty("4", bodyTexture);
        textures(item).addProperty("particle", bodyTexture);
        return item;
    }

    private static void smartTextures(JsonObject model, SmartFluidPipeBuilder builder, boolean item) {
        boolean nativeModel = item || !customStationary(builder);
        for (int i = 1; i <= 3; i++)
            if (nativeModel || explicit(builder, "smart_pipe_" + i, "body", "body_" + i))
                textures(model).addProperty(i == 3 ? item ? "4" : "5" : String.valueOf(i + 1), builder.bodyTexture(i));
        if (nativeModel || explicit(builder, "pipe"))
            textures(model).addProperty(item ? "1" : "4", builder.pipeTexture());
        if (nativeModel || explicit(builder, "smart_pipe_3", "body", "body_3"))
            textures(model).addProperty("particle", builder.bodyTexture(3));
        rawTextures(model, builder);
    }

    private static void valveBodyTextures(JsonObject model, FluidValveBuilder builder) {
        textures(model).addProperty("2", builder.bodyTexture());
        textures(model).addProperty("particle", builder.bodyTexture());
    }

    private static void pipeAttachmentTexture(JsonObject model, FluidValveBuilder builder) {
        // Dynamic collars are supplied by PipeAttachmentModel; keep their atlas declared here
        // even though the stationary valve mesh does not contain those faces.
        if (!customStationary(builder) || explicit(builder, "pipe"))
            textures(model).addProperty("cbjs_pipe", builder.pipeTexture());
    }

    private static JsonObject textures(JsonObject model) {
        if (!model.has("textures")) model.add("textures", new JsonObject());
        return model.getAsJsonObject("textures");
    }

    private static void rawTextures(JsonObject model, BlockBuilder builder) {
        for (var entry : builder.textures.entrySet()) {
            String slot = entry.getKey();
            if (slot.equals("pump") || slot.equals("cog") || slot.equals("body") || slot.equals("pipe")
                    || slot.equals("body_1") || slot.equals("body_2") || slot.equals("body_3")
                    || slot.equals("smart_pipe_1") || slot.equals("smart_pipe_2") || slot.equals("smart_pipe_3")
                    || slot.equals("fluid_valve")) continue;
            textures(model).add(slot, entry.getValue());
        }
    }

    private static void finish(BlockBuilder builder, AssetJsonGenerator generator, JsonObject state, JsonObject item) {
        finish(builder, generator, state, item, true);
    }

    private static void finish(BlockBuilder builder, AssetJsonGenerator generator, JsonObject state, JsonObject item,
                               boolean applyItemParent) {
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(),
                builder.blockstateJson == null ? state : builder.blockstateJson);
        if (builder.itemBuilder == null) return;
        item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson,
                applyItemParent ? builder.itemBuilder.parentModel : "", builder.itemBuilder.textureJson);
        GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
    }
}
