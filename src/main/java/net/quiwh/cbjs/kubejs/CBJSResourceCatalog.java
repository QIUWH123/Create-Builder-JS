package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
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
    public record Entry(CBJSBlockBuilder builder, String type, ResourceLocation source,
            List<String> textureRoles, List<String> modelRoles) {
        public boolean generated() { return source != null; }
    }

    public static List<Entry> registered() {
        return RegistryObjectStorage.BLOCK.objects.values().stream()
                .filter(value -> value instanceof CBJSBlockBuilder)
                .map(value -> describe((CBJSBlockBuilder) value)).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(entry -> entry.builder().id.toString())).toList();
    }

    public static Entry describe(CBJSBlockBuilder builder) {
        if (builder instanceof CogwheelBuilder gear) {
            String type = gear.large ? "large_" : "";
            type += gear.flanged ? "flanged_cogwheel" : gear.shaftless ? "shaftless_cogwheel" : "cogwheel";
            return entry(builder, type, gear.base == null ? null : gear.base.id,
                    "gear shaft shaft_top", "cogwheel shaft shaft_half");
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
                "block connected encased_cogwheel_side encased_cogwheel_side_connected gearbox encased_pipe", "");
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
        if (builder instanceof ShaftPoweredBuilder shaft) return entry(builder, "generated_powered_shaft", shaft.shaft.id, "", "");
        if (builder instanceof LitBlazeBurnerBuilder burner) return entry(builder, "generated_lit_blaze_burner", burner.source.id, "", "");
        if (builder instanceof SteamWhistleExtensionBuilder extension) return entry(builder, "generated_steam_whistle_extension", extension.whistle.id, "", "");
        if (builder instanceof BeltFunnelBuilder funnel) return entry(builder, "generated_belt_funnel", funnel.funnel.id, "", "");
        if (builder instanceof ChainConveyorBuilder) return entry(builder, "chain_conveyor", null,
                "casing guard wheel axis axis_top all", "block body shaft wheel guard item");
        if (builder instanceof BlazeBurnerBuilder burner) return new Entry(builder, burner.genericBurner() ? "burner" : "blaze_burner", null,
                words("brazier brazier_soul blaze blaze_active blaze_super blaze_super_active inert rods rods_2 super_rods super_rods_2 flame flame_scroll super_flame_scroll fire soul_fire goggles goggles_small train_hat logistics_hat particle all"),
                roles("block body burner item", BlazeBurnerBuilder.PARTS));
        if (builder instanceof SteamEngineBuilder) return entry(builder, "steam_engine", null,
                "engine linkage all", "block body engine piston linkage connector item");
        if (builder instanceof SteamWhistleBuilder) return new Entry(builder, "steam_whistle", null,
                words("whistle plate powered all"), roles("block body whistle item", SteamAssets.WHISTLE_PARTS));
        if (builder instanceof NativeMachineBuilder machine) {
            String nativeName = machine.nativeBlockId().getPath();
            var parts = new java.util.TreeSet<>(machine.nativeParts().keySet());
            if (builder instanceof FunnelBuilder) parts.add("belt");
            return new Entry(builder, nativeName, null,
                    roles("all body item", machine.nativeParts().keySet()),
                    roles("block body item " + nativeName, parts));
        }
        // Keep optional integration classes unloaded unless that mod is present. They still
        // participate in validation, but their generated forms are never registration templates.
        if (builder.getClass().getName().startsWith("net.quiwh.cbjs.compat."))
            return entry(builder, "generated_compatibility_variant", builder.id, "", "");
        return null;
    }

    private static Entry entry(CBJSBlockBuilder builder, String type, ResourceLocation source, String textures, String models) {
        return new Entry(builder, type, source, words(textures), words(models));
    }
    private static List<String> words(String values) { return values.isEmpty() ? List.of() : List.of(values.split(" ")); }
    private static List<String> roles(String defaults, Collection<String> additional) {
        var roles = new java.util.TreeSet<>(words(defaults));
        roles.addAll(additional);
        return List.copyOf(roles);
    }

    public static Map<ResourceLocation, JsonObject> generate(Entry entry) throws IOException {
        var data = new LinkedHashMap<ResourceLocation, GeneratedData>();
        entry.builder().generateAssets(new KubeAssetGenerator() {
            @Override public dev.latvian.mods.kubejs.util.RegistryAccessContainer getRegistries() {
                return dev.latvian.mods.kubejs.util.RegistryAccessContainer.current;
            }
            @Override public void add(GeneratedData generated) { data.put(generated.id(), generated); }
            @Override public GeneratedData getGenerated(ResourceLocation id) { return data.get(id); }
        });
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
        result.addProperty("supportsSingleModel", !(entry.builder() instanceof CogwheelBuilder) && !entry.modelRoles().isEmpty());
        result.add("explicitTextures", CBJSAssets.json(entry.builder().textures));
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
        if (builder instanceof FluidDeviceBuilder device && device.allTexture != null)
            text.append("\n    .textureAll(").append(quote(device.allTexture)).append(")");
        for (var slot : builder.textures.entrySet())
            text.append("\n    .replaceTexture(")
                    .append(quote(slot.getKey())).append(", ")
                    .append(quote(slot.getValue())).append(")");
        ModelOverrides overrides = overrides(builder);
        if (!(builder instanceof CogwheelBuilder) && !entry.modelRoles().isEmpty() && builder.model != null && !builder.model.isEmpty()) {
            text.append("\n    .model(");
            if (overrides != null && !overrides.usesPrimaryItem() && entry.modelRoles().contains("body"))
                text.append("\"body\", ");
            text.append(quote(builder.model)).append(")");
        }
        if (overrides != null) for (String part : entry.modelRoles()) if (overrides.has(part))
            text.append("\n    .model(").append(quote(part)).append(", ").append(quote(overrides.get(part))).append(")");
        if (builder.itemBuilder != null
                && CBJSAssets.parent(builder.itemBuilder) != null && !CBJSAssets.parent(builder.itemBuilder).isEmpty()
                && (overrides == null || !overrides.usesPrimaryItem())) {
            if (builder instanceof CogwheelBuilder)
                text.append("\n    .item(item => item.parentModel(").append(quote(CBJSAssets.parent(builder.itemBuilder))).append("))");
            else if (entry.modelRoles().contains("item"))
                text.append("\n    .model(\"item\", ").append(quote(CBJSAssets.parent(builder.itemBuilder))).append(")");
        }
        if (builder.itemBuilder == null) text.append("\n    .noItem()");
        text.append("\n})\n");
        return text.toString();
    }

    private static ModelOverrides overrides(CBJSBlockBuilder builder) {
        if (builder instanceof CogwheelBuilder gear) return gear.modelOverrides();
        if (builder instanceof ShaftBuilder shaft) return shaft.modelOverrides;
        if (builder instanceof GearboxBuilder gearbox) return gearbox.modelOverrides;
        if (builder instanceof TransmissionBuilder transmission) return transmission.modelOverrides;
        if (builder instanceof ChainDriveBuilder drive) return drive.modelOverrides;
        if (builder instanceof ChainBuilder chain) return chain.modelOverrides;
        if (builder instanceof PumpBuilder pump) return pump.modelOverrides;
        if (builder instanceof SmartFluidPipeBuilder pipe) return pipe.modelOverrides;
        if (builder instanceof ChainConveyorBuilder conveyor) return conveyor.modelOverrides;
        if (builder instanceof BlazeBurnerBuilder burner) return burner.modelOverrides;
        if (builder instanceof SteamEngineBuilder engine) return engine.modelOverrides;
        if (builder instanceof SteamWhistleBuilder whistle) return whistle.modelOverrides;
        if (builder instanceof NativeMachineBuilder machine) return machine.modelOverrides;
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
