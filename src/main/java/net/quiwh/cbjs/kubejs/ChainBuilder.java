package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedChainBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** One material chain block/item usable by vanilla placement and supported mechanical systems. */
public final class ChainBuilder extends BlockBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "chain"), java.util.Set.of());
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private java.util.function.Consumer<LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public ChainBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public ChainBuilder(ResourceLocation id) {
        super(id);
        textures.entrySet().clear();
        hardness = 5;
        resistance = 6;
        soundType = SoundType.CHAIN;
        mapColor(MapColor.METAL);
        requiresTool = true;
        notSolid = true;
        opaque = false;
        fullBlock = false;
        suffocating = false;
        viewBlocking = false;
        redstoneConductor = false;
        renderType = "cutout_mipped";
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
        getOrCreateItemBuilder();
    }

    public String chainTexture() {
        if (textures.has("chain")) return textures.get("chain").getAsString();
        if (textures.has("all")) return textures.get("all").getAsString();
        return "minecraft:block/chain";
    }

    @Override public ChainBuilder textureAll(String texture) {
        super.textureAll(texture);
        textures.addProperty("chain", texture);
        return this;
    }

    public String itemTexture() {
        if (textures.has("item")) return textures.get("item").getAsString();
        if (textures.has("chain") || textures.has("all")) return chainTexture();
        return "minecraft:item/chain";
    }

    public ChainBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }

    /** Removes this material's RPM rating; endpoint blocks retain their own kinetic limits. */
    public ChainBuilder noMaxRPM() {
        maxRpm = null;
        independentRpmLimit = true;
        return this;
    }

    public ChainBuilder maxStress(int value) {
        maxStress = checked("maxStress", value);
        return this;
    }

    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }

    @Override public ChainBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }

    public ChainBuilder model(String role, String path) {
        modelOverrides.set(role, path);
        return this;
    }

    private void validateBlockEntity() {
        if (blockEntityInfo != null)
            throw new IllegalArgumentException("Vanilla chain blocks do not support custom block entities: " + id);
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedChainBlock(this);
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        // The placed chain is decorative. Its rule is consumed by mechanical connection checks.
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }

    @Override public ChainBuilder noDrops() {
        super.noDrops();
        dropsDisabled = true;
        disabledLoot = lootTable;
        return this;
    }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) { ChainAssets.generate(this, generator); }

    @Override public void generateDataJsons(DataJsonGenerator generator) {
        com.google.gson.JsonObject loot;
        if (lootTable != null && lootTable != disabledLoot) {
            var custom = new LootBuilder(null);
            custom.type = "minecraft:block";
            lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(dropsDisabled || itemBuilder == null ? null : id);
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_tables/blocks/" + id.getPath()), loot);
    }
}
