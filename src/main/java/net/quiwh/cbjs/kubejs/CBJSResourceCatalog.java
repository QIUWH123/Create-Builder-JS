package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.data.GeneratedData;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** On-demand builder metadata for resource commands; never creates registry objects. */
public final class CBJSResourceCatalog {
    public record Entry(BlockBuilder builder, String type, ResourceLocation source,
            List<String> textureRoles, List<String> modelRoles) {
        public boolean generated() { return source != null; }
    }

    public static List<Entry> registered() {
        return RegistryInfo.BLOCK.objects.values().stream()
                .filter(value -> value instanceof BlockBuilder)
                .map(value -> describe((BlockBuilder) value)).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(entry -> entry.builder().id.toString())).toList();
    }

    public static Entry describe(BlockBuilder builder) {
        if (builder instanceof CogwheelBuilder gear) {
            String type = gear.large ? "large_" : "";
            type += gear.shaftless ? "shaftless_cogwheel" : "cogwheel";
            return entry(builder, type, gear.base == null ? null : gear.base.id,
                    "gear shaft shaft_top", "cogwheel shaft shaft_half item");
        }
        if (builder instanceof ShaftBuilder) return entry(builder, "shaft", null, "axis axis_top", "block body shaft item");
        if (builder instanceof ShaftEncasedBuilder shaft) return entry(builder, "generated_encased_shaft", shaft.shaft.id, "", "");
        if (builder instanceof CasingVariantBuilder variant) return entry(builder, "generated_casing_variant", variant.casing.id, "", "");
        if (builder instanceof GearboxBuilder gearbox) return entry(builder,
                gearbox.directional() ? "six_way_gearbox" : gearbox.vertical ? "vertical_gearbox" : "gearbox",
                null, "casing gearbox", "block body gearbox shaft shaft_half item");
        if (builder instanceof TransmissionBuilder transmission) return entry(builder, transmission.kind.path, null,
                transmission.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER
                        ? "side top gearbox frame brass casing bracket axis axis_top" : "side powered gearbox frame axis axis_top",
                transmission.kind == TransmissionBuilder.Kind.SPEED_CONTROLLER
                        ? "block body shaft bracket item" : "block body powered shaft_half item");
        if (builder instanceof ChainDriveBuilder) return entry(builder, "encased_chain_drive", null,
                "side end middle gearbox axis axis_top", "block body single end_horizontal end_vertical middle_horizontal middle_vertical shaft item");
        if (builder instanceof ChainBuilder) return entry(builder, "chain", null, "chain item", "block body chain item");
        if (builder instanceof CasingBuilder) return entry(builder, "casing", null,
                "casing casing_connected cogwheel_side cogwheel_side_connected shaft pipe", "");
        if (builder instanceof MechanicalBeltBuilder) return entry(builder, "mechanical_belt", null,
                "belt belt_offset belt_diagonal belt_scroll belt_diagonal_scroll item", "");
        if (builder instanceof FluidPipeBuilder pipe) return entry(builder, "fluid_pipe", pipe.base == null ? null : pipe.base.id,
                "pipe pipe_connected glass", "");
        if (builder instanceof PumpBuilder) return entry(builder, "mechanical_pump", null, "pump cog pipe", "block body pump cog cogwheel item");
        if (builder instanceof SmartFluidPipeBuilder) return entry(builder, "smart_pipe", null,
                "smart_pipe_1 smart_pipe_2 smart_pipe_3 pipe", "block body item");
        if (builder instanceof FluidValveBuilder) return entry(builder, "fluid_valve", null, "fluid_valve pipe", "");
        if (builder instanceof FluidTankBuilder tank) return new Entry(builder, tank.isVessel() ? "fluid_vessel" : "fluid_tank", null,
                FluidTankBuilder.TEXTURE_ROLES.stream().sorted().toList(), List.of());
        if (builder instanceof ItemVaultBuilder vault) return new Entry(builder, vault.vertical() ? "item_silo" : "item_vault", null,
                ItemVaultBuilder.TEXTURE_ROLES.stream().sorted().toList(), List.of());
        // Keep optional integration classes unloaded unless that mod is present. They still
        // participate in validation, but their generated forms are never registration templates.
        if (builder.getClass().getName().startsWith("net.quiwh.cbjs.compat."))
            return entry(builder, "generated_compatibility_variant", builder.id, "", "");
        return null;
    }

    private static Entry entry(BlockBuilder builder, String type, ResourceLocation source, String textures, String models) {
        return new Entry(builder, type, source, words(textures), words(models));
    }
    private static List<String> words(String values) { return values.isEmpty() ? List.of() : List.of(values.split(" ")); }

    public static Map<ResourceLocation, JsonObject> generate(Entry entry) throws IOException {
        var data = new LinkedHashMap<ResourceLocation, GeneratedData>();
        entry.builder().generateAssetJsons(new AssetJsonGenerator(data));
        var result = new LinkedHashMap<ResourceLocation, JsonObject>();
        for (var file : data.entrySet()) try (var input = file.getValue().get();
                var reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            result.put(file.getKey(), JsonParser.parseReader(reader).getAsJsonObject());
        }
        return result;
    }

    public static JsonObject manifest(Entry entry) {
        var result = new JsonObject();
        result.addProperty("block", entry.builder().id.toString());
        result.addProperty("registrationType", entry.type());
        result.addProperty("generatedCompanion", entry.generated());
        if (entry.source() != null) result.addProperty("sourceRegistration", entry.source().toString());
        result.add("textureRoles", strings(entry.textureRoles()));
        result.add("namedModelRoles", strings(entry.modelRoles()));
        result.addProperty("supportsSingleModel", !entry.modelRoles().isEmpty());
        result.add("explicitTextures", entry.builder().textures.deepCopy());
        result.addProperty("note", "JSON templates only. Existing Create/vanilla/custom PNGs and external models remain resource references. This is not a full registration backup.");
        return result;
    }

    public static String registration(Entry entry) {
        var builder = entry.builder();
        if (entry.generated()) return "// " + builder.id + " is generated by " + entry.source()
                + ".\n// Edit/export the source registration instead of registering this companion again.\n";
        var text = new StringBuilder("// Resource template for ").append(builder.id)
                .append(". Replace your existing registration; do not register its ID twice.\n")
                .append("// Copy assets/ into kubejs/assets/. PNGs and referenced dependency models stay in their owning packs.\n")
                .append("// This snippet captures resource choices, not gameplay settings/callbacks.\n")
                .append("StartupEvents.registry('block', event => {\n  event.create(")
                .append(quote(builder.id.toString())).append(", ").append(quote(entry.type())).append(")");
        // Explicit assignments are retained; default roles and all transitive dependencies
        // are listed in manifest.json rather than inventing filenames for unused slots.
        for (var slot : builder.textures.entrySet()) if (slot.getValue().isJsonPrimitive())
            text.append("\n    .replaceTexture(").append(quote(slot.getKey())).append(", ")
                    .append(quote(slot.getValue().getAsString())).append(")");
        ModelOverrides overrides = overrides(builder);
        if (!entry.modelRoles().isEmpty() && builder.model != null && !builder.model.isEmpty()) {
            text.append("\n    .model(");
            if (overrides != null && !overrides.usesPrimaryItem() && entry.modelRoles().contains("body"))
                text.append("\"body\", ");
            text.append(quote(builder.model)).append(")");
        }
        if (overrides != null) for (String part : entry.modelRoles()) if (overrides.has(part))
            text.append("\n    .model(").append(quote(part)).append(", ").append(quote(overrides.get(part))).append(")");
        if (entry.modelRoles().contains("item") && builder.itemBuilder != null
                && builder.itemBuilder.parentModel != null && !builder.itemBuilder.parentModel.isEmpty()
                && (overrides == null || !overrides.usesPrimaryItem()))
            text.append("\n    .model(\"item\", ").append(quote(builder.itemBuilder.parentModel)).append(")");
        if (builder.itemBuilder == null) text.append("\n    .noItem()");
        text.append("\n})\n");
        return text.toString();
    }

    private static ModelOverrides overrides(BlockBuilder builder) {
        if (builder instanceof CogwheelBuilder gear) return gear.modelOverrides();
        if (builder instanceof ShaftBuilder shaft) return shaft.modelOverrides;
        if (builder instanceof GearboxBuilder gearbox) return gearbox.modelOverrides;
        if (builder instanceof TransmissionBuilder transmission) return transmission.modelOverrides;
        if (builder instanceof ChainDriveBuilder drive) return drive.modelOverrides;
        if (builder instanceof ChainBuilder chain) return chain.modelOverrides;
        if (builder instanceof PumpBuilder pump) return pump.modelOverrides;
        if (builder instanceof SmartFluidPipeBuilder pipe) return pipe.modelOverrides;
        return null;
    }

    public static JsonArray strings(Collection<?> values) {
        var result = new JsonArray();
        values.stream().map(Object::toString).sorted().forEach(result::add);
        return result;
    }
    private static String quote(String value) { return new com.google.gson.Gson().toJson(value); }
    private CBJSResourceCatalog() { }
}
