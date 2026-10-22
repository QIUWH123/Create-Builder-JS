package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** A normal KubeJS block builder whose block keeps Create's kinetic mechanics. */
public class CogwheelBuilder extends BlockBuilder {
    public final boolean large;
    public final boolean shaftless;
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    public String importedModel;
    private boolean encased = true;
    private boolean chainable;
    private ModelOverrides modelOverrides;
    private boolean dropsDisabled;
    private java.util.function.Consumer<dev.latvian.mods.kubejs.loot.LootBuilder> disabledLoot;
    String allTexture;
    private boolean additionalCreated;
    final CogwheelBuilder base;
    final boolean brass;

    /** Replace one texture slot while preserving the family's default geometry. */
    public CogwheelBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public CogwheelBuilder(ResourceLocation id, boolean large) { this(id, large, false); }

    public CogwheelBuilder(ResourceLocation id, boolean large, boolean shaftless) {
        this(id, large, shaftless, null, false);
    }

    private CogwheelBuilder(ResourceLocation id, boolean large, boolean shaftless, CogwheelBuilder base, boolean brass) {
        super(id);
        this.large = large;
        this.shaftless = shaftless;
        this.base = base;
        this.brass = brass;
        textures.entrySet().clear();
        allTexture = null;
        // Create cogwheels copy stone strength (1.5 / 6), then use wood sound and dirt map color.
        hardness = 1.5F;
        resistance = 6F;
        mapColor(net.minecraft.world.level.material.MapColor.DIRT);
        suffocating = false;
        viewBlocking = false;
        opaque = false;
        fullBlock = false;
        // Match Create's noOcclusion: the casing must admit world light to its rotating parts.
        notSolid = true;
        renderType = "cutout_mipped";
        itemBuilder = new GearItemBuilder(id);
        itemBuilder.blockBuilder = this;
    }

    @Override public CogwheelBuilder textureAll(String texture) {
        super.textureAll(texture);
        allTexture = texture;
        return this;
    }
    @Override public CogwheelBuilder noDrops() {
        super.noDrops();
        dropsDisabled = true;
        disabledLoot = lootTable;
        return this;
    }
    @Override public BlockBuilder item(java.util.function.Consumer<BlockItemBuilder> consumer) {
        super.item(consumer);
        if (consumer == null) { dropsDisabled = true; disabledLoot = lootTable; }
        return this;
    }
    @Override public void generateDataJsons(dev.latvian.mods.kubejs.generator.DataJsonGenerator generator) {
        generateLoot(generator, id);
    }

    void generateLoot(dev.latvian.mods.kubejs.generator.DataJsonGenerator generator, ResourceLocation target) {
        CogwheelBuilder source = base == null ? this : base;
        // Emit an explicit empty table too, so noDrops remains effective after reload.
        com.google.gson.JsonObject loot;
        if (source.lootTable != null && source.lootTable != source.disabledLoot) {
            var custom = new dev.latvian.mods.kubejs.loot.LootBuilder(null);
            custom.type = "minecraft:block";
            source.lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(source.dropsDisabled || source.itemBuilder == null ? null : source.id);
        generator.json(ResourceLocation.fromNamespaceAndPath(target.getNamespace(), "loot_tables/blocks/" + target.getPath()), loot);
    }

    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (itemBuilder == null) {
            itemBuilder = new GearItemBuilder(id);
            itemBuilder.blockBuilder = this;
        }
        return itemBuilder;
    }

    public CogwheelBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }
    public CogwheelBuilder noMaxRPM() {
        maxRpm = null;
        independentRpmLimit = true;
        return this;
    }
    public CogwheelBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }
    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }
    @Override public CogwheelBuilder model(String path) {
        path = ModelOverrides.validPath(path);
        String preset = GearAssets.presetFromModel(path, shaftless);
        if (preset == null) {
            // Keep any previously selected rotating/casing preset, as modelSet(...).model(...) did.
            super.model(path);
            modelOverrides().primarySelected();
        } else {
            importedModel = preset;
            model = "";
            if (itemBuilder != null) itemBuilder.parentModel = "";
        }
        return this;
    }
    /** Select the shaftless wheel, optional shaft parts, or an independent inventory model. */
    public CogwheelBuilder model(String part, String path) {
        modelOverrides().set(part, path);
        return this;
    }
    ModelOverrides modelOverrides() {
        if (modelOverrides == null)
            modelOverrides = new ModelOverrides(this, java.util.Set.of(),
                    java.util.Set.of("cogwheel", "shaft", "shaft_half"));
        return modelOverrides;
    }
    public CogwheelBuilder modelSet(String material) {
        GearAssets.validatePreset(material);
        importedModel = material;
        return this;
    }
    public CogwheelBuilder noEncasedVariants() { encased = false; return this; }

    /** Opt into Bits 'n' Bobs cogwheel chains when the optional addon is present. */
    public CogwheelBuilder chainable() { return chainable(true); }
    public CogwheelBuilder chainable(boolean enabled) { chainable = enabled; return this; }
    public boolean chainableEnabled() { return base == null && chainable; }

    boolean encasedVariantsEnabled() { return encased; }

    public GearModelSet models() {
        CogwheelBuilder source = base == null ? this : base;
        GearModelSet generated = GearModelSet.generated(source.id,
                !source.shaftless && GearAssets.customShaftTextures(source));
        String prefix = "block/" + source.id.getPath() + "/";
        return new GearModelSet(generated.small(), generated.large(),
                !source.shaftless && source.modelOverrides().has("shaft") ? GearAssets.path(source.id, prefix + "shaft") : generated.shaft(),
                !source.shaftless && source.modelOverrides().has("shaft_half") ? GearAssets.path(source.id, prefix + "shaft_half") : generated.shaftHalf(),
                source.modelOverrides().customPrimary() ? GearAssets.path(source.id, "block/" + source.id.getPath()) : null);
    }

    @Override public Block createObject() {
        if (blockEntityInfo != null) throw new IllegalArgumentException("Cogwheels own their Create block entity");
        if (base == null) return new ScriptedCogwheelBlock(this);
        return new ScriptedEncasedCogwheelBlock(this, brass ? AllBlocks.BRASS_CASING::get : AllBlocks.ANDESITE_CASING::get,
                () -> (MaterialCogwheelBlock) base.get());
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        if (blockEntityInfo != null) throw new IllegalArgumentException("Cogwheels own their Create block entity");
        additionalCreated = true;
        super.createAdditionalObjects();
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
        if (base != null) {
            ModBlockEntities.addEncasedValidBlock(this);
            ModCogwheels.addEncased(() -> (MaterialCogwheelBlock) base.get(),
                    () -> (MaterialEncasedCogwheelBlock) get(), brass);
        } else {
            ModBlockEntities.addCogwheelValidBlock(this);
            ModCogwheels.addBase(() -> (MaterialCogwheelBlock) get());
            if (chainable && net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
                net.quiwh.cbjs.compat.bnb.BnbChainCompat.add(this);
            if (encased) {
                companion(false);
                companion(true);
            }
        }
    }

    private void companion(boolean brass) {
        var child = new CogwheelBuilder(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                (brass ? "brass_encased_" : "andesite_encased_") + id.getPath()), large, shaftless, this, brass);
        child.maxRpm = maxRpm;
        child.maxStress = maxStress;
        child.independentRpmLimit = independentRpmLimit;
        child.importedModel = importedModel;
        CompanionInheritance.copy(this, child, false);
        RegistryInfo.BLOCK.addBuilder(child);
        // KubeJS snapshots the initial block builder collection before this callback.
        child.createAdditionalObjects();
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { GearAssets.generate(this, generator); }

    private final class GearItemBuilder extends BlockItemBuilder {
        GearItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return base == null ? new CogwheelBlockItem((MaterialCogwheelBlock) blockBuilder.get(), createItemProperties())
                : new net.minecraft.world.item.BlockItem(blockBuilder.get(), createItemProperties()); }
    }
}
