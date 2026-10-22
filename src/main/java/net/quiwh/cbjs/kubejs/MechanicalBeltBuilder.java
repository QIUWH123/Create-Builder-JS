package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.quiwh.cbjs.content.ScriptedBeltBlockEntities;
import net.quiwh.cbjs.content.ScriptedBeltConnectorItem;
import net.quiwh.cbjs.content.ScriptedMechanicalBeltBlock;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** A native Create belt family with one matching connector and script-owned textures/limits. */
public final class MechanicalBeltBuilder extends BlockBuilder {
    public Integer maxRpm;
    public Integer maxStress;
    public Integer maxLength;
    public Float itemSpacing;
    public boolean independentRpmLimit;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private java.util.function.Consumer<LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public MechanicalBeltBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public MechanicalBeltBuilder(ResourceLocation id) {
        super(id);
        textures.entrySet().clear();
        hardness = .8F;
        resistance = .8F;
        soundType = SoundType.WOOL;
        mapColor(MapColor.COLOR_GRAY);
        notSolid = true;
        opaque = false;
        fullBlock = false;
        suffocating = false;
        viewBlocking = false;
        renderType = "cutout_mipped";
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
        getOrCreateItemBuilder();
    }

    public String texturePath(String role) {
        if (textures.has(role)) return textures.get(role).getAsString();
        return switch (role) {
            case "belt", "belt_offset", "belt_diagonal", "belt_scroll", "belt_diagonal_scroll" -> "create:block/" + role;
            case "item" -> "create:item/belt_connector";
            default -> throw new IllegalArgumentException("Unknown mechanical belt texture role: " + role);
        };
    }

    public MechanicalBeltBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }
    public MechanicalBeltBuilder noMaxRPM() { maxRpm = null; independentRpmLimit = true; return this; }
    public MechanicalBeltBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }
    /** Maximum occupied belt blocks, including both end pulleys. Omitted uses Create's current cap. */
    public MechanicalBeltBuilder maxLength(int value) {
        if (value < 2)
            throw new IllegalArgumentException("maxLength must be at least 2 belt blocks, including both pulleys");
        maxLength = value;
        return this;
    }
    /** Ignore Create's length cap for this material. A later maxLength call sets a cap again. */
    public MechanicalBeltBuilder noMaxLength() { maxLength = Integer.MAX_VALUE; return this; }
    /** Minimum distance between transported stack centers, in Create belt segments. */
    public MechanicalBeltBuilder itemSpacing(double value) {
        float spacing = (float) value;
        if (!Float.isFinite(spacing) || spacing <= 0)
            throw new IllegalArgumentException("itemSpacing must be a positive finite number representable as a float");
        itemSpacing = spacing;
        return this;
    }
    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }

    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof ConnectorBuilder)) {
            itemBuilder = new ConnectorBuilder(id);
            itemBuilder.blockBuilder = this;
        }
        return itemBuilder;
    }
    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedMechanicalBeltBlock(this);
    }
    private void validateBlockEntity() {
        if (blockEntityInfo != null)
            throw new IllegalArgumentException("Mechanical belts own their Create block entity: " + id);
    }
    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        ModMechanicalBelts.add(this);
        ScriptedBeltBlockEntities.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }
    @Override public MechanicalBeltBuilder noDrops() {
        super.noDrops();
        dropsDisabled = true;
        disabledLoot = lootTable;
        return this;
    }
    @Override public void generateAssetJsons(AssetJsonGenerator generator) { MechanicalBeltAssets.generate(this, generator); }
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

    private final class ConnectorBuilder extends BlockItemBuilder {
        ConnectorBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() {
            return new ScriptedBeltConnectorItem(blockBuilder.get(), createItemProperties());
        }
    }
}
