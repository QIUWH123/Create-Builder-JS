package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** A normal KubeJS block builder whose block keeps Create's kinetic mechanics. */
public class CogwheelBuilder extends CBJSBlockBuilder {
    public final boolean large;
    public final boolean shaftless;
    public final boolean flanged;
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean encased = true;
    private boolean chain;
    private boolean rope;
    private boolean belt;
    private ModelOverrides modelOverrides;
    private boolean dropsDisabled;
    private java.util.function.Consumer<net.quiwh.cbjs.kubejs.LootBuilder> disabledLoot;
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
        this(id, large, shaftless, false);
    }

    public CogwheelBuilder(ResourceLocation id, boolean large, boolean shaftless, boolean flanged) {
        this(id, large, shaftless, flanged, null, false);
    }

    private CogwheelBuilder(ResourceLocation id, boolean large, boolean shaftless, boolean flanged, CogwheelBuilder base, boolean brass) {
        super(id);
        if (flanged && shaftless) throw new IllegalArgumentException("Flanged cogwheels require a shaft");
        this.large = large;
        this.shaftless = shaftless;
        this.flanged = flanged;
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
        renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        itemBuilder = new GearItemBuilder(id);
        ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        if (flanged) {
            // Resource IDs are safe without BnB; no optional class is linked here.
            tagBlock(ResourceLocation.parse("bits_n_bobs:flanged_cogwheel"));
            tagBlock(ResourceLocation.parse("bits_n_bobs:" + (large ? "large" : "small") + "_flanged_cogwheel"));
            tagBlock(ResourceLocation.parse("bits_n_bobs:cogwheel_chain_no_small_offset"));
            if (base == null) tagBlock(ResourceLocation.parse("bits_n_bobs:dedicated_cogwheel_chain_component"));
        }
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
    @Override public BlockBuilder item(java.util.function.Consumer<dev.latvian.mods.kubejs.item.ItemBuilder> consumer) {
        super.item(consumer);
        if (consumer == null) { dropsDisabled = true; disabledLoot = lootTable; }
        return this;
    }
    @Override public void generateData(dev.latvian.mods.kubejs.generator.KubeDataGenerator generator) {
        generateLoot(generator, id);
    }

    void generateLoot(dev.latvian.mods.kubejs.generator.KubeDataGenerator generator, ResourceLocation target) {
        CogwheelBuilder source = base == null ? this : base;
        // Emit an explicit empty table too, so noDrops remains effective after reload.
        com.google.gson.JsonObject loot;
        if (source.lootTable != null && source.lootTable != source.disabledLoot) {
            var custom = new net.quiwh.cbjs.kubejs.LootBuilder(null);
            custom.type = "minecraft:block";
            source.lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(source.dropsDisabled || source.itemBuilder == null ? null : source.id);
        generator.json(ResourceLocation.fromNamespaceAndPath(target.getNamespace(), "loot_table/blocks/" + target.getPath()), loot);
    }

    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (itemBuilder == null) {
            itemBuilder = new GearItemBuilder(id);
            ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this;
        }
        return (BlockItemBuilder) itemBuilder;
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
    @HideFromJS
    @Override public CogwheelBuilder model(String path) {
        throw new IllegalArgumentException("Cogwheels use model('cogwheel', path), model('shaft', path) or model('shaft_half', path)");
    }
    @HideFromJS
    @Override public CogwheelBuilder parentModel(ResourceLocation parent) {
        throw new IllegalArgumentException("Cogwheels use named part models; customize inventory with item(item => item.parentModel(path))");
    }
    @HideFromJS
    @Override public CogwheelBuilder modelGenerator(java.util.function.Consumer<dev.latvian.mods.kubejs.client.ModelGenerator> generator) {
        throw new IllegalArgumentException("Cogwheels use named part models; customize inventory with item(item => item.modelGenerator(...))");
    }
    /** Select a shaftless wheel or an optional shaft part; inventory uses the native item callback. */
    public CogwheelBuilder model(String part, String path) {
        if ("item".equals(part))
            throw new IllegalArgumentException("Set a cogwheel item model with item(item => item.parentModel(path))");
        if (!"cogwheel".equals(part) && !"shaft".equals(part) && !"shaft_half".equals(part))
            throw new IllegalArgumentException("Unknown cogwheel model role '" + part + "'; supported roles: cogwheel, shaft, shaft_half");
        modelOverrides().set(part, path);
        return this;
    }
    ModelOverrides modelOverrides() {
        if (modelOverrides == null)
            modelOverrides = new ModelOverrides(this, java.util.Set.of(),
                    java.util.Set.of("cogwheel", "shaft", "shaft_half"));
        return modelOverrides;
    }
    public CogwheelBuilder noEncasedVariants() { encased = false; return this; }

    /** Opt into cogwheel drives, with a built-in backend when Bits 'n' Bobs is absent. */
    public CogwheelBuilder chain() { chain = true; return this; }
    public CogwheelBuilder rope() { rope = true; return this; }
    public CogwheelBuilder belt() { belt = true; return this; }

    @HideFromJS public boolean chainEnabled() { return base == null && chain; }
    @HideFromJS public boolean ropeEnabled() { return base == null && rope; }
    @HideFromJS public boolean beltEnabled() { return base == null && belt; }
    @HideFromJS public boolean anyDriveEnabled() { return chainEnabled() || ropeEnabled() || beltEnabled(); }

    boolean encasedVariantsEnabled() { return encased; }

    public GearModelSet models() {
        CogwheelBuilder source = base == null ? this : base;
        GearModelSet generated = GearModelSet.generated(source.id,
                !source.shaftless && GearAssets.customShaftTextures(source));
        String prefix = "block/" + source.id.getPath() + "/";
        return new GearModelSet(generated.small(), generated.large(),
                !source.shaftless && source.modelOverrides().has("shaft") ? GearAssets.path(source.id, prefix + "shaft") : generated.shaft(),
                !source.shaftless && source.modelOverrides().has("shaft_half") ? GearAssets.path(source.id, prefix + "shaft_half") : generated.shaftHalf());
    }

    void validateModels() {
        if (model != null && !model.isEmpty() || modelJson != null || modelGenerator != null)
            throw new IllegalArgumentException("Cogwheel " + id
                    + " uses separate cogwheel/shaft/shaft_half models; complete block model overrides are unsupported. Use item(...) for inventory overrides");
    }

    @Override public Block createObject() {
        validateModels();
        if (blockEntityInfo != null) throw new IllegalArgumentException("Cogwheels own their Create block entity");
        if (base == null) return new ScriptedCogwheelBlock(this);
        return new ScriptedEncasedCogwheelBlock(this, brass ? AllBlocks.BRASS_CASING::get : AllBlocks.ANDESITE_CASING::get,
                () -> (MaterialCogwheelBlock) base.get());
    }

    /**
     * KubeJS may construct a block item before the block registry supplier runs.
     * Materialize this builder once and reuse that object for both registry paths.
     */
    @Override public Block createTransformedObject() {
        if (object != null) return object;
        if (base != null) base.createTransformedObject();
        return super.createTransformedObject();
    }

    @Override public void createAdditionalObjects(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (additionalCreated) return;
        validateModels();
        if (blockEntityInfo != null) throw new IllegalArgumentException("Cogwheels own their Create block entity");
        additionalCreated = true;
        super.createAdditionalObjects(registry);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
        if (base != null) {
            ModBlockEntities.addEncasedValidBlock(this);
            ModCogwheels.addEncased(() -> (MaterialCogwheelBlock) base.get(),
                    () -> (MaterialEncasedCogwheelBlock) get(), brass);
        } else {
            ModBlockEntities.addCogwheelValidBlock(this);
            ModCogwheels.addBase(() -> (MaterialCogwheelBlock) get());
            if (anyDriveEnabled() && net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
                net.quiwh.cbjs.compat.bnb.BnbChainCompat.add(this);
            if (encased) {
                companion(false, registry);
                companion(true, registry);
            }
        }
    }

    private void companion(boolean brass, dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        var child = new CogwheelBuilder(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                (brass ? "brass_encased_" : "andesite_encased_") + id.getPath()), large, shaftless, flanged, this, brass);
        child.maxRpm = maxRpm;
        child.maxStress = maxStress;
        child.independentRpmLimit = independentRpmLimit;
        CompanionInheritance.copy(this, child, false);
        registry.add(net.minecraft.core.registries.Registries.BLOCK, child);
        // KubeJS snapshots the initial block builder collection before this callback.
        child.createAdditionalObjects(registry);
    }

    @Override public void generateAssets(KubeAssetGenerator generator) { GearAssets.generate(this, generator); }

    private final class GearItemBuilder extends CBJSBlockItemBuilder {
        GearItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() {
            Block block = CogwheelBuilder.this.createTransformedObject();
            return base == null ? new CogwheelBlockItem((MaterialCogwheelBlock) block, createItemProperties())
                    : new net.minecraft.world.item.BlockItem(block, createItemProperties());
        }
    }
}
