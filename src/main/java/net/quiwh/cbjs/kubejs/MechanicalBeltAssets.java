package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;

/** Script-owned aliases reuse the installed Create belt geometry and placement rotations. */
final class MechanicalBeltAssets {
    private static final String[] PARTS = {"start", "middle", "end", "start_bottom", "middle_bottom",
            "end_bottom", "diagonal_start", "diagonal_middle", "diagonal_end"};

    static void generate(MechanicalBeltBuilder builder, AssetJsonGenerator generator) {
        String root = "block/" + builder.id.getPath();
        for (String part : PARTS) {
            JsonObject model = GearAssets.parent("create:block/belt/" + part);
            boolean bottom = part.endsWith("bottom");
            String role = part.startsWith("diagonal") ? "belt_diagonal" : bottom ? "belt_offset" : "belt";
            JsonObject textures = new JsonObject();
            textures.addProperty(bottom ? "1" : "0", builder.texturePath(role));
            textures.addProperty("particle", builder.texturePath(role));
            model.add("textures", textures);
            GearAssets.put(generator, builder.id, "models", root + "/" + part, model);
        }

        JsonObject particle = builder.modelJson != null ? builder.modelJson.deepCopy()
                : GearAssets.parent(builder.model != null && !builder.model.isEmpty()
                        ? builder.model : "create:block/belt/particle");
        if (!particle.has("textures")) particle.add("textures", new JsonObject());
        JsonObject textures = particle.getAsJsonObject("textures");
        if (builder.textures.has("particle")) textures.add("particle", builder.textures.get("particle"));
        else if (builder.textures.has("belt") || builder.modelJson == null && (builder.model == null || builder.model.isEmpty()))
            textures.addProperty("particle", builder.texturePath("belt"));
        // Scroll targets are not faces in the stationary mesh, but must enter the block atlas.
        textures.addProperty("cbjs_belt_scroll", builder.texturePath("belt_scroll"));
        textures.addProperty("cbjs_belt_diagonal_scroll", builder.texturePath("belt_diagonal_scroll"));
        GearAssets.put(generator, builder.id, "models", root + "/particle", particle);

        JsonObject state = ScriptModelResources.loadBlockstate(ResourceLocation.parse("create:belt"));
        for (var entry : state.getAsJsonObject("variants").entrySet()) {
            JsonObject variant = entry.getValue().getAsJsonObject();
            if (variant.get("model").getAsString().equals("create:block/belt/particle"))
                variant.addProperty("model", builder.id.getNamespace() + ":" + root + "/particle");
        }
        // Casing models retain their native sprite: BeltModel remaps brass to andesite at runtime.
        GearAssets.put(generator, builder.id, "blockstates", builder.id.getPath(),
                builder.blockstateJson == null ? state : builder.blockstateJson);
        if (builder.itemBuilder != null) {
            JsonObject item = GearAssets.parent("create:item/belt_connector");
            JsonObject itemTextures = new JsonObject();
            itemTextures.addProperty("layer0", builder.texturePath("item"));
            item.add("textures", itemTextures);
            item = GearAssets.itemOverrides(item, builder.itemBuilder.modelJson,
                    builder.itemBuilder.parentModel, builder.itemBuilder.textureJson);
            GearAssets.put(generator, builder.id, "models/item", builder.id.getPath(), item);
        }
    }

    private MechanicalBeltAssets() { }
}
