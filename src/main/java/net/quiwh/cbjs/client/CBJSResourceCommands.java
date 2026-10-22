package net.quiwh.cbjs.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kubejs.CBJSResourceCatalog;
import net.quiwh.cbjs.resources.CBJSResourceAudit;
import net.quiwh.cbjs.resources.CBJSResourceTemplates;

/** Local, explicitly requested validation/export, using the currently loaded resource packs. */
@Mod.EventBusSubscriber(modid = CBJS.MOD_ID, value = Dist.CLIENT)
public final class CBJSResourceCommands {
    private static final ResourceLocation ATLAS = ResourceLocation.parse("minecraft:atlases/blocks.json");

    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) { register(event.getDispatcher()); }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cbjs").then(Commands.literal("resources")
                .then(Commands.literal("validate").executes(context -> validate(context.getSource())))
                .then(Commands.literal("export").then(Commands.argument("block_id", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                CBJSResourceCatalog.registered().stream().map(entry -> entry.builder().id), builder))
                        .executes(context -> export(context.getSource(), ResourceLocationArgument.getId(context, "block_id")))))));
    }

    private static int validate(CommandSourceStack source) {
        var entries = CBJSResourceCatalog.registered();
        var generated = new LinkedHashMap<ResourceLocation, JsonObject>();
        var generationIssues = new JsonArray();
        for (var entry : entries) try { generated.putAll(CBJSResourceCatalog.generate(entry)); }
        catch (RuntimeException | IOException exception) {
            var issue = new JsonObject(); issue.addProperty("source", entry.builder().id.toString());
            issue.addProperty("message", "Asset generation failed: " + exception.getMessage()); generationIssues.add(issue);
        }
        try {
            var resources = new ResourceView(Minecraft.getInstance().getResourceManager(), generated);
            var report = CBJSResourceAudit.validate(generated.keySet(), resources::read, resources::exists, resources.aliases());
            var json = reportJson(report); json.addProperty("registeredBlocks", entries.size());
            for (var issue : generationIssues) json.getAsJsonArray("issues").add(issue);
            int failures = json.getAsJsonArray("issues").size();
            var output = CBJSResourceTemplates.writeReport(base().resolve("reports"), json);
            String status = "Create Builder JS: checked " + entries.size() + " blocks, " + report.models().size()
                    + " models and " + report.textureFiles().size() + " textures; " + failures + " issue(s). Report: " + output;
            if (failures == 0) source.sendSuccess(() -> Component.literal(status), false);
            else {
                source.sendFailure(Component.literal(status));
                for (int i = 0; i < Math.min(5, json.getAsJsonArray("issues").size()); i++) {
                    var issue = json.getAsJsonArray("issues").get(i).getAsJsonObject();
                    source.sendFailure(Component.literal(issue.get("source").getAsString() + ": " + issue.get("message").getAsString()));
                }
            }
            return failures == 0 ? 1 : 0;
        } catch (RuntimeException | IOException exception) { return failed(source, exception); }
    }

    private static int export(CommandSourceStack source, ResourceLocation block) {
        var entry = CBJSResourceCatalog.registered().stream().filter(value -> value.builder().id.equals(block)).findFirst().orElse(null);
        if (entry == null) { source.sendFailure(Component.literal("No Create Builder JS block registration: " + block)); return 0; }
        try {
            var generated = new LinkedHashMap<>(CBJSResourceCatalog.generate(entry));
            var resources = new ResourceView(Minecraft.getInstance().getResourceManager(), generated);
            var template = prepareExport(entry, generated, resources);
            Path output = CBJSResourceTemplates.export(base(), block, template.files(), template.manifest(), template.registration());
            source.sendSuccess(() -> Component.literal("Create Builder JS resource template: " + output
                    + (template.report().issues().isEmpty() ? "" : " (" + template.report().issues().size() + " validation issue(s); see manifest.json)")), false);
            return 1;
        } catch (RuntimeException | IOException exception) { return failed(source, exception); }
    }

    public record Template(Map<ResourceLocation, JsonObject> files, JsonObject manifest,
            String registration, CBJSResourceAudit.Report report) { }

    /** Pure preparation is separate from command execution and filesystem mutation. */
    public static Template prepareExport(CBJSResourceCatalog.Entry entry, Map<ResourceLocation, JsonObject> generated,
            ResourceView resources) {
        var report = CBJSResourceAudit.validate(generated.keySet(), resources::read, resources::exists, resources.aliases());
        var files = new LinkedHashMap<ResourceLocation, JsonObject>();
        // Retain active overrides for generated files and reachable custom JSON dependencies.
        // Create/vanilla models and every PNG remain references to their owning resource packs.
        for (var file : generated.keySet()) if (!file.equals(ATLAS)) {
            var active = resources.read(file); if (active != null) files.put(file, active);
        }
        for (var file : report.models()) if (!file.getNamespace().equals("create") && !file.getNamespace().equals("minecraft")) {
            var model = resources.read(file); if (model != null) files.put(file, model);
        }
        var sprites = new JsonArray();
        for (var sprite : report.textures().stream().sorted().toList()) {
            var texture = resources.aliases().get(sprite);
            if (texture == null) continue;
            var single = new JsonObject(); single.addProperty("type", "minecraft:single");
            single.addProperty("sprite", sprite.toString()); single.addProperty("resource", texture.toString()); sprites.add(single);
        }
        if (!sprites.isEmpty()) { var atlas = new JsonObject(); atlas.add("sources", sprites); files.put(ATLAS, atlas); }
        var manifest = CBJSResourceCatalog.manifest(entry);
        manifest.add("generatedFiles", CBJSResourceCatalog.strings(files.keySet()));
        manifest.add("validation", reportJson(report));
        return new Template(Map.copyOf(files), manifest, CBJSResourceCatalog.registration(entry), report);
    }

    private static int failed(CommandSourceStack source, Exception exception) {
        source.sendFailure(Component.literal("Create Builder JS resources: " + exception.getMessage()));
        return 0;
    }

    private static Path base() throws IOException {
        // Canonicalize the game root (macOS /var itself is a system symlink), then refuse
        // user-controlled symlinks in the export subtree.
        return FMLPaths.GAMEDIR.get().toRealPath().resolve("kubejs/exported/cbjs");
    }

    public static JsonObject reportJson(CBJSResourceAudit.Report report) {
        var result = new JsonObject(); var issues = new JsonArray();
        for (var issue : report.issues()) {
            var json = new JsonObject(); json.addProperty("source", issue.source().toString());
            json.addProperty("message", issue.message()); issues.add(json);
        }
        result.add("issues", issues);
        result.add("models", CBJSResourceCatalog.strings(report.models()));
        result.add("sprites", CBJSResourceCatalog.strings(report.textures()));
        result.add("textureFiles", CBJSResourceCatalog.strings(report.textureFiles()));
        return result;
    }

    /** Active resource overrides take priority; generated JSON supplies only missing files. */
    public static final class ResourceView {
        private final ResourceManager resources;
        private final Map<ResourceLocation, JsonObject> generated;
        private Map<ResourceLocation, ResourceLocation> aliases;
        public ResourceView(ResourceManager resources, Map<ResourceLocation, JsonObject> generated) {
            this.resources = resources;
            this.generated = Map.copyOf(generated);
        }
        public JsonObject read(ResourceLocation id) {
            if (!CBJSResourceAudit.safe(id)) throw new IllegalArgumentException("Unsafe resource path " + id);
            var resource = resources.getResource(id);
            if (resource.isEmpty()) return generated.get(id);
            try (var reader = resource.get().openAsReader()) { return JsonParser.parseReader(reader).getAsJsonObject(); }
            catch (IOException exception) { throw new UncheckedIOException(exception); }
        }
        public boolean exists(ResourceLocation id) { return CBJSResourceAudit.safe(id) && resources.getResource(id).isPresent(); }
        public Map<ResourceLocation, ResourceLocation> aliases() {
            if (aliases != null) return aliases;
            var atlasStack = new ArrayList<JsonObject>();
            for (var resource : resources.getResourceStack(ATLAS)) try (var reader = resource.openAsReader()) {
                atlasStack.add(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException exception) { throw new UncheckedIOException(exception); }
            if (atlasStack.isEmpty() && generated.containsKey(ATLAS)) atlasStack.add(generated.get(ATLAS));
            aliases = CBJSResourceAudit.singleSpriteSources(atlasStack);
            return aliases;
        }
    }

    private CBJSResourceCommands() { }
}
