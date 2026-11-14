package net.quiwh.cbjs.resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** Writes reviewable templates without changing the running pack or replacing prior exports. */
public final class CBJSResourceTemplates {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Path export(Path base, ResourceLocation block, Map<ResourceLocation, JsonObject> resources,
            JsonObject manifest, String registration) throws IOException {
        requireSafe(block);
        for (var file : resources.keySet()) {
            requireSafe(file);
            if (!file.getPath().endsWith(".json")) throw new IllegalArgumentException("Only JSON templates are exported: " + file);
        }
        Path absolute = base.toAbsolutePath().normalize();
        Path target = absolute.resolve(block.getNamespace()).resolve(block.getPath()).normalize();
        if (!target.startsWith(absolute)) throw new IllegalArgumentException("Export escapes template directory: " + block);
        checkAncestors(target);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) throw new java.nio.file.FileAlreadyExistsException(target.toString());
        Files.createDirectories(target.getParent());
        checkAncestors(target.getParent());
        Path staging = Files.createTempDirectory(target.getParent(), ".cbjs-template-");
        try {
            for (var entry : resources.entrySet()) {
                var id = entry.getKey();
                var output = staging.resolve("assets").resolve(id.getNamespace()).resolve(id.getPath());
                Files.createDirectories(output.getParent());
                write(output, JSON.toJson(entry.getValue()) + "\n");
            }
            write(staging.resolve("manifest.json"), JSON.toJson(manifest) + "\n");
            write(staging.resolve("registration.js"), registration);
            // No REPLACE_EXISTING: an earlier export (or a concurrently created target) wins.
            Files.move(staging, target);
            return target;
        } finally {
            if (Files.exists(staging)) try (var paths = Files.walk(staging)) {
                for (var path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    /** Reports also use a fresh filename, with the same ancestor/symlink checks as exports. */
    public static Path writeReport(Path base, JsonObject report) throws IOException {
        Path absolute = base.toAbsolutePath().normalize();
        checkAncestors(absolute);
        Files.createDirectories(absolute);
        checkAncestors(absolute);
        var output = Files.createTempFile(absolute, "validation-", ".json");
        Files.writeString(output, JSON.toJson(report) + "\n", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
        return output;
    }

    private static void requireSafe(ResourceLocation id) {
        if (!CBJSResourceAudit.safe(id)) throw new IllegalArgumentException("Unsafe resource path " + id);
    }
    private static void write(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }
    private static void checkAncestors(Path path) throws IOException {
        for (Path current = path; current != null; current = current.getParent())
            if (Files.isSymbolicLink(current)) throw new IOException("Template path contains a symbolic link: " + current);
    }
    private CBJSResourceTemplates() { }
}
