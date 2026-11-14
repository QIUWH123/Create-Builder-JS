package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Checks that the six-way gearbox's default model is shipped with CBJS. */
public final class GearboxVariantAssetsTest {
    public static void main(String[] args) throws IOException {
        String expected = "cbjs:block/cogwheel/six_way_gearbox/block";
        if (!expected.equals(GearboxVariantAssets.DEFAULT_MODEL))
            throw new AssertionError("Unexpected six-way model parent: " + GearboxVariantAssets.DEFAULT_MODEL);

        String resource = "/assets/cbjs/models/block/cogwheel/six_way_gearbox/block.json";
        try (var stream = GearboxVariantAssetsTest.class.getResourceAsStream(resource)) {
            if (stream == null) throw new AssertionError("Missing packaged model resource " + resource);
            var model = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!model.has("elements") || model.getAsJsonArray("elements").isEmpty())
                throw new AssertionError("Packaged six-way gearbox model has no geometry");
            if (!model.getAsJsonObject("textures").get("0").getAsString().equals("create:block/gearbox"))
                throw new AssertionError("Packaged six-way gearbox model has the wrong atlas texture");
        }
        System.out.println("GearboxVariantAssetsTest: model parent and packaged geometry verified");
    }

    private GearboxVariantAssetsTest() { }
}
