package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.Create;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** Reads editable KubeJS models first, then the owning Forge mod's installed resources. */
final class ScriptModelResources {
    private ScriptModelResources() { }

    private static InputStream open(ResourceLocation model) throws IOException {
        return open(model, "models");
    }

    private static InputStream open(ResourceLocation model, String directory) throws IOException {
        String relative = model.getNamespace() + "/" + directory + "/" + model.getPath() + ".json";
        Path game = FMLLoader.getGamePath();
        Path assets = (game == null ? Path.of("") : game).resolve("kubejs/assets").toAbsolutePath().normalize();
        Path local = assets.resolve(relative).normalize();
        if (!local.startsWith(assets)) throw new IllegalArgumentException("Invalid model path " + model);
        if (Files.isRegularFile(local)) return Files.newInputStream(local);
        // Forge isolates mod class loaders. Read another mod through its mod file, not ours.
        var mods = FMLLoader.getLoadingModList();
        if (mods != null) {
            var owner = mods.getModFileById(model.getNamespace());
            if (owner != null) {
                Path resource = owner.getFile().findResource("assets/" + relative);
                if (Files.isRegularFile(resource)) return Files.newInputStream(resource);
            }
            // Unknown namespaces may still be supplied by a classpath resource
            // (for example a verification fixture or an embedded resource pack).
            // Keep the classpath fallback below reachable when no mod file owns it.
        }
        // Standalone verification has no Forge mod list; use the resource's owning class.
        Class<?> owner = model.getNamespace().equals("create") ? Create.class : ScriptModelResources.class;
        return owner.getResourceAsStream("/assets/" + relative);
    }

    static boolean exists(ResourceLocation model) {
        try (InputStream stream = open(model)) {
            return stream != null;
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    static JsonObject load(ResourceLocation model) {
        return load(model, "models");
    }

    static JsonObject loadBlockstate(ResourceLocation block) {
        return load(block, "blockstates");
    }

    private static JsonObject load(ResourceLocation model, String directory) {
        try (InputStream stream = open(model, directory)) {
            if (stream == null) throw new IllegalArgumentException("Missing " + directory + " resource " + model + " (check kubejs/assets)");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) { throw new UncheckedIOException(exception); }
    }

    static JsonObject resolved(ResourceLocation model) {
        return resolved(model, new HashSet<>());
    }

    static JsonObject resolved(JsonObject model) {
        return inherit(model.deepCopy(), new HashSet<>());
    }

    private static JsonObject resolved(ResourceLocation model, Set<ResourceLocation> parents) {
        if (!parents.add(model)) throw new IllegalArgumentException("Cyclic model parent " + model);
        try { return inherit(load(model), parents); }
        finally { parents.remove(model); }
    }

    private static JsonObject inherit(JsonObject own, Set<ResourceLocation> parents) {
        if (!own.has("parent")) return own;
        ResourceLocation parent = ResourceLocation.parse(own.get("parent").getAsString());
        // Leave vanilla lighting/display parents to Minecraft's resource manager.
        if (parent.getNamespace().equals("minecraft")) return own;
        JsonObject inherited = resolved(parent, parents);
        for (var entry : own.entrySet()) {
            if (entry.getKey().equals("parent")) continue;
            if (entry.getKey().equals("textures") && inherited.has("textures")) {
                for (var texture : entry.getValue().getAsJsonObject().entrySet())
                    inherited.getAsJsonObject("textures").add(texture.getKey(), texture.getValue());
            } else inherited.add(entry.getKey(), entry.getValue());
        }
        return inherited;
    }
}
