package net.quiwh.cbjs.compat.bnb;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import net.minecraft.resources.ResourceLocation;

/** The hidden form shares its source's stationary and moving material resources. */
public final class BnbChainedCogwheelAssets {
    public static void generate(BnbChainedCogwheelBuilder builder, AssetJsonGenerator generator) {
        var source = builder.source();
        JsonObject state;
        if (source.blockstateJson != null) {
            state = source.blockstateJson.deepCopy();
        } else {
            JsonObject variants = new JsonObject();
            for (String axis : new String[]{"x", "y", "z"}) {
                JsonObject model = new JsonObject();
                model.addProperty("model", source.id.getNamespace() + ":block/" + source.id.getPath());
                if (!axis.equals("y")) {
                    model.addProperty("x", 90);
                    model.addProperty("y", axis.equals("x") ? 90 : 180);
                }
                variants.add("axis=" + axis, model);
            }
            state = new JsonObject();
            state.add("variants", variants);
        }
        generator.json(ResourceLocation.fromNamespaceAndPath(builder.id.getNamespace(),
                "blockstates/" + builder.id.getPath()), state);
    }

    private BnbChainedCogwheelAssets() { }
}
