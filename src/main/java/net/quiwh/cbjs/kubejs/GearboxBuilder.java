package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.quiwh.cbjs.content.ScriptedGearboxBlock;
import net.quiwh.cbjs.content.ScriptedGearboxBlockEntities;
import net.quiwh.cbjs.content.ScriptedDirectionalGearboxBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;
import java.util.function.Consumer;

/** Scripted gearboxes with native Create or owned directional transmission. */
public final class GearboxBuilder extends BlockBuilder {
    public enum Kind { STANDARD, VERTICAL, SIX_WAY }
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "gearbox"), java.util.Set.of("shaft"));
    public final Kind kind;
    public final boolean vertical;
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private Consumer<LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public GearboxBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public GearboxBuilder(ResourceLocation id, boolean vertical) {
        this(id, vertical ? Kind.VERTICAL : Kind.STANDARD);
    }
    public GearboxBuilder(ResourceLocation id, Kind kind) {
        super(id);
        this.kind = java.util.Objects.requireNonNull(kind, "gearbox kind");
        this.vertical = kind == Kind.VERTICAL;
        textures.entrySet().clear();
        hardness = 1.5F;
        resistance = 6;
        soundType = SoundType.WOOD;
        notSolid = true;
        opaque = false;
        suffocating = false;
        viewBlocking = false;
        renderType = "cutout_mipped";
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
    }
    @Override public GearboxBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }
    public GearboxBuilder model(String role, String path) {
        modelOverrides.set("shaft_half".equals(role) ? "shaft" : role, path);
        return this;
    }
    public boolean directional() { return kind == Kind.SIX_WAY; }
    public String casingTexture() { return textureOrDefault("casing", kind == Kind.SIX_WAY
            ? "create:block/gearbox" : "create:block/andesite_casing"); }
    public String gearboxTexture() { return textureOrDefault("gearbox", "create:block/gearbox"); }
    private String textureOrDefault(String role, String fallback) {
        if (textures.has(role)) return textures.get(role).getAsString();
        if (textures.has("all")) return textures.get("all").getAsString();
        if (textures.has("north")) return textures.get("north").getAsString();
        return fallback;
    }
    public GearboxBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }
    public GearboxBuilder noMaxRPM() { maxRpm = null; independentRpmLimit = true; return this; }
    public GearboxBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }
    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }
    private void validateBlockEntity() {
        if (blockEntityInfo != null) throw new IllegalArgumentException("Gearboxes own their Create block entity: " + id);
    }
    @Override public Block createObject() {
        validateBlockEntity();
        return directional() ? new ScriptedDirectionalGearboxBlock(this) : new ScriptedGearboxBlock(this);
    }
    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        ScriptedGearboxBlockEntities.add(this);
        ModGearboxes.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }
    @Override public GearboxBuilder noDrops() {
        super.noDrops(); dropsDisabled = true; disabledLoot = lootTable; return this;
    }
    @Override public void generateDataJsons(DataJsonGenerator generator) {
        JsonObject loot;
        if (lootTable != null && lootTable != disabledLoot) {
            var custom = new LootBuilder(null);
            custom.type = "minecraft:block";
            lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(dropsDisabled || itemBuilder == null ? null : id);
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_tables/blocks/" + id.getPath()), loot);
    }
    JsonObject gearboxModel(boolean item) {
        JsonObject result = item ? GearAssets.parent("create:block/gearbox/" + (vertical ? "item_vertical" : "item"))
                : modelOverrides.primaryModel("create:block/gearbox/block");
        return textureModel(result, item && vertical ? "gearbox_top" : "0", item && vertical ? "gearbox" : "1",
                item || !modelOverrides.customPrimary());
    }
    boolean textureSpecified(String role) {
        return textures.has(role) || textures.has("all") || textures.has("north");
    }
    JsonObject textureModel(JsonObject model, String casingSlot, String gearboxSlot, boolean nativeModel) {
        JsonObject slots = model.has("textures") ? model.getAsJsonObject("textures") : new JsonObject();
        if (nativeModel || textureSpecified("casing")) {
            slots.addProperty(casingSlot, casingTexture());
            slots.addProperty("particle", casingTexture());
        }
        if (nativeModel || textureSpecified("gearbox")) slots.addProperty(gearboxSlot, gearboxTexture());
        for (var entry : textures.entrySet())
            if (!java.util.Set.of("casing", "gearbox", "all", "north").contains(entry.getKey()))
                slots.add(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
        return model;
    }
    void generateShaftModel(AssetJsonGenerator generator) {
        if (modelOverrides.has("shaft")) GearAssets.put(generator, id, "models", "block/" + id.getPath() + "/shaft_half",
                modelOverrides.partModel("shaft", "create:block/shaft_half"));
    }
    JsonObject itemModel(JsonObject nativeItem) {
        boolean sharedPrimary = modelOverrides.usesPrimaryItem();
        if (sharedPrimary) {
            nativeItem = kind == Kind.SIX_WAY ? GearboxVariantAssets.textureModel(this, GearAssets.parent(model))
                    : textureModel(GearAssets.parent(model), "0", "1", false);
        }
        return GearAssets.itemOverrides(nativeItem, itemBuilder.modelJson,
                sharedPrimary ? null : itemBuilder.parentModel, itemBuilder.textureJson);
    }
    @Override public void generateAssetJsons(AssetJsonGenerator generator) {
        generateShaftModel(generator);
        if (directional()) {
            GearboxVariantAssets.generate(this, generator);
            return;
        }
        GearAssets.put(generator, id, "models", "block/" + id.getPath() + "/block", gearboxModel(false));
        GearAssets.put(generator, id, "blockstates", id.getPath(), blockstateJson == null
                ? CasingAssets.axisState(id, true) : blockstateJson);
        if (itemBuilder != null) GearAssets.put(generator, id, "models/item", id.getPath(), itemModel(gearboxModel(true)));
    }
}
