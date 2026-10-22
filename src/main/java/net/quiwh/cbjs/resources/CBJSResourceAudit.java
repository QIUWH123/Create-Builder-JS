package net.quiwh.cbjs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;

/** An on-demand resource graph audit. It does no work during rendering or world ticks. */
public final class CBJSResourceAudit {
    public record Issue(ResourceLocation source, String message) { }
    public record Report(List<Issue> issues, Set<ResourceLocation> models,
            Set<ResourceLocation> textures, Set<ResourceLocation> textureFiles) { }

    private final Function<ResourceLocation, JsonObject> reader;
    private final Predicate<ResourceLocation> exists;
    private final Map<ResourceLocation, ResourceLocation> spriteSources;
    private final Map<ResourceLocation, JsonObject> loaded = new HashMap<>();
    private final Map<ResourceLocation, JsonObject> resolved = new HashMap<>();
    private final Set<ResourceLocation> unavailable = new HashSet<>();
    private final Set<ResourceLocation> checked = new HashSet<>();
    private final Set<ResourceLocation> models = new LinkedHashSet<>();
    private final Set<ResourceLocation> textures = new LinkedHashSet<>();
    private final Set<ResourceLocation> textureFiles = new LinkedHashSet<>();
    private final Set<Issue> issues = new LinkedHashSet<>();

    private CBJSResourceAudit(Function<ResourceLocation, JsonObject> reader,
            Predicate<ResourceLocation> exists, Map<ResourceLocation, ResourceLocation> spriteSources) {
        this.reader = reader;
        this.exists = exists;
        this.spriteSources = Map.copyOf(spriteSources);
    }

    /** Roots use real resource paths, e.g. example:models/block/shaft.json. */
    public static Report validate(Collection<ResourceLocation> roots,
            Function<ResourceLocation, JsonObject> reader, Predicate<ResourceLocation> exists,
            Map<ResourceLocation, ResourceLocation> spriteSources) {
        var audit = new CBJSResourceAudit(reader, exists, spriteSources);
        for (var root : roots) {
            if (!safe(root)) {
                audit.issue(root, "Unsafe resource path " + root);
                continue;
            }
            if (root.getPath().startsWith("models/") && root.getPath().endsWith(".json"))
                audit.checkModel(root, new LinkedHashSet<>());
            else if (root.getPath().startsWith("blockstates/")) {
                var json = audit.load(root);
                if (json != null) audit.modelReferences(root, json, new LinkedHashSet<>());
            }
        }
        return new Report(List.copyOf(audit.issues), Set.copyOf(audit.models),
                Set.copyOf(audit.textures), Set.copyOf(audit.textureFiles));
    }

    private void issue(ResourceLocation source, String message) { issues.add(new Issue(source, message)); }

    private JsonObject load(ResourceLocation file) {
        if (loaded.containsKey(file)) return loaded.get(file);
        if (unavailable.contains(file)) return null;
        try {
            var json = reader.apply(file);
            if (json != null) {
                loaded.put(file, json);
                return json;
            }
            issue(file, "Missing model/resource " + file);
        } catch (RuntimeException exception) {
            issue(file, "Cannot read JSON " + file + ": " + exception.getMessage());
        }
        unavailable.add(file);
        return null;
    }

    private void checkModel(ResourceLocation file, Set<ResourceLocation> active) {
        if (active.contains(file)) { issue(file, "Cyclic model reference " + file); return; }
        if (active.size() > 128) { issue(file, "Model dependency depth exceeds 128: " + file); return; }
        if (!checked.add(file)) return;
        var model = inherit(file, active);
        if (model != null) {
            active.add(file);
            try { checkGeometry(file, model, new JsonObject(), active, 0); }
            finally { active.remove(file); }
        }
    }

    private JsonObject inherit(ResourceLocation file, Set<ResourceLocation> active) {
        if (resolved.containsKey(file)) return resolved.get(file);
        if (!active.add(file)) {
            issue(file, "Cyclic model parent/reference " + file);
            return null;
        }
        if (active.size() > 128) {
            issue(file, "Model dependency depth exceeds 128: " + file);
            active.remove(file);
            return null;
        }
        try {
            models.add(file);
            var own = load(file);
            if (own == null) return null;
            var result = inheritInline(file, own, active);
            resolved.put(file, result);
            return result;
        } finally { active.remove(file); }
    }

    private JsonObject inheritInline(ResourceLocation file, JsonObject own, Set<ResourceLocation> active) {
        var result = new JsonObject();
        if (own.has("parent")) {
            var parent = reference(file, own.get("parent"), "parent model");
            if (parent != null && !builtin(parent)) {
                var inherited = inherit(modelFile(parent), active);
                if (inherited != null) result = inherited.deepCopy();
            }
        }
        for (var entry : own.entrySet()) {
            if (entry.getKey().equals("textures") && entry.getValue().isJsonObject()
                    && result.has("textures") && result.get("textures").isJsonObject()) {
                for (var slot : entry.getValue().getAsJsonObject().entrySet())
                    result.getAsJsonObject("textures").add(slot.getKey(), slot.getValue());
            } else if (!entry.getKey().equals("parent")) result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void checkGeometry(ResourceLocation source, JsonObject model, JsonObject outerTextures,
            Set<ResourceLocation> active, int depth) {
        if (depth > 128) { issue(source, "Inline model depth exceeds 128"); return; }
        var slots = outerTextures.deepCopy();
        if (model.has("textures")) {
            if (!model.get("textures").isJsonObject()) issue(source, "Invalid model textures object");
            else for (var entry : model.getAsJsonObject("textures").entrySet()) slots.add(entry.getKey(), entry.getValue());
        }
        // Validate every assigned slot, including item layers and particles with no cuboid faces.
        for (var slot : slots.entrySet()) checkTexture(source, slot.getValue(), slots, new LinkedHashSet<>());
        if (model.has("elements")) checkFaceTextures(source, model.get("elements"), slots, 0);
        for (var key : List.of("children", "parts")) if (model.has(key) && model.get(key).isJsonObject())
            for (var child : model.getAsJsonObject(key).entrySet()) if (child.getValue().isJsonObject()) {
                var own = child.getValue().getAsJsonObject();
                var inherited = inheritInline(source, own, active);
                checkGeometry(source, inherited, slots, active, depth + 1);
            }
        // Item overrides and custom loaders can introduce additional referenced models.
        modelReferences(source, model, active);
    }

    private void checkFaceTextures(ResourceLocation source, JsonElement json, JsonObject slots, int depth) {
        if (depth > 128) { issue(source, "Model element depth exceeds 128"); return; }
        if (json.isJsonObject()) for (var entry : json.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("texture")) checkTexture(source, entry.getValue(), slots, new LinkedHashSet<>());
            else checkFaceTextures(source, entry.getValue(), slots, depth + 1);
        } else if (json.isJsonArray()) for (var child : json.getAsJsonArray()) checkFaceTextures(source, child, slots, depth + 1);
    }

    private void modelReferences(ResourceLocation source, JsonElement json, Set<ResourceLocation> active) {
        modelReferences(source, json, active, 0);
    }

    private void modelReferences(ResourceLocation source, JsonElement json, Set<ResourceLocation> active, int depth) {
        if (depth > 128) { issue(source, "JSON dependency depth exceeds 128"); return; }
        if (json.isJsonObject()) for (var entry : json.getAsJsonObject().entrySet()) {
            if (entry.getKey().equals("model") && entry.getValue().isJsonPrimitive()) {
                var reference = reference(source, entry.getValue(), "model");
                if (reference != null && !builtin(reference)) {
                    var file = modelFile(reference);
                    checkModel(file, active);
                }
            } else if (!entry.getKey().equals("textures") && !entry.getKey().equals("elements"))
                modelReferences(source, entry.getValue(), active, depth + 1);
        } else if (json.isJsonArray()) for (var child : json.getAsJsonArray()) modelReferences(source, child, active, depth + 1);
    }

    private void checkTexture(ResourceLocation source, JsonElement value, JsonObject slots, Set<String> variables) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            issue(source, "Invalid texture reference " + value);
            return;
        }
        String name = value.getAsString();
        if (name.startsWith("#")) {
            String variable = name.substring(1);
            if (!variables.add(variable)) { issue(source, "Cyclic texture variable " + name); return; }
            if (!slots.has(variable)) { issue(source, "Unresolved texture variable " + name); return; }
            checkTexture(source, slots.get(variable), slots, variables);
            return;
        }
        var sprite = reference(source, value, "texture");
        if (sprite == null) return;
        textures.add(sprite);
        if (sprite.equals(ResourceLocation.parse("minecraft:missingno"))) return;
        // A single-file atlas source reads its resource directly from textures/, even if that
        // name is also a sprite alias. It does not recursively resolve other sprite names.
        var physical = spriteSources.getOrDefault(sprite, sprite);
        if (!safe(physical)) { issue(source, "Unsafe resource path " + physical); return; }
        var png = ResourceLocation.fromNamespaceAndPath(physical.getNamespace(), "textures/" + physical.getPath() + ".png");
        if (textureFiles.add(png) && !exists.test(png)) issue(source, "Missing texture " + png + (physical.equals(sprite) ? "" : " (sprite " + sprite + ")"));
    }

    private ResourceLocation reference(ResourceLocation source, JsonElement value, String kind) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            issue(source, "Invalid " + kind + " reference " + value); return null;
        }
        var id = ResourceLocation.tryParse(value.getAsString());
        if (id == null) { issue(source, "Invalid " + kind + " resource ID " + value); return null; }
        if (!safe(id)) { issue(source, "Unsafe resource path " + id); return null; }
        return id;
    }

    private static boolean builtin(ResourceLocation id) {
        return id.getNamespace().equals("minecraft") && id.getPath().startsWith("builtin/");
    }

    private static ResourceLocation modelFile(ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "models/" + id.getPath() + ".json");
    }

    /** ResourceLocation allows '..' and repeated slashes; exported paths must not. */
    public static boolean safe(ResourceLocation id) {
        if (id.getNamespace().equals(".") || id.getNamespace().equals("..")) return false;
        String path = id.getPath();
        if (path.startsWith("/") || path.endsWith("/")) return false;
        for (String segment : path.split("/", -1))
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) return false;
        return true;
    }

    /** Merge the ordered atlas resource stack, preserving actual single-file source semantics. */
    public static Map<ResourceLocation, ResourceLocation> singleSpriteSources(List<JsonObject> atlasStack) {
        var result = new LinkedHashMap<ResourceLocation, ResourceLocation>();
        for (var atlas : atlasStack) if (atlas.has("sources") && atlas.get("sources").isJsonArray())
            for (var value : atlas.getAsJsonArray("sources")) {
                if (!value.isJsonObject()) continue;
                var source = value.getAsJsonObject();
                if (!source.has("type") || !source.has("resource")) continue;
                String type = source.get("type").getAsString();
                if (!type.equals("single") && !type.equals("minecraft:single")) continue;
                var resource = ResourceLocation.tryParse(source.get("resource").getAsString());
                var sprite = source.has("sprite") ? ResourceLocation.tryParse(source.get("sprite").getAsString()) : resource;
                if (sprite != null && resource != null) result.put(sprite, resource);
            }
        return Map.copyOf(result);
    }

    private CBJSResourceAudit() { throw new AssertionError(); }
}
