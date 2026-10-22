package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.quiwh.cbjs.content.ScriptedChainDriveBlock;
import net.quiwh.cbjs.content.ScriptedChainDriveBlockEntities;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Native Create chain drive behavior with script-selected materials, models and limits. */
public final class ChainDriveBuilder extends BlockBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            Set.of("block", "body"), Set.of("single", "end_horizontal", "end_vertical",
                    "middle_horizontal", "middle_vertical", "shaft"));
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private Consumer<LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public ChainDriveBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public ChainDriveBuilder(ResourceLocation id) {
        super(id);
        textures.entrySet().clear();
        hardness = 1.5F;
        resistance = 6;
        soundType = SoundType.STONE;
        notSolid = true;
        opaque = false;
        suffocating = false;
        viewBlocking = false;
        renderType = "cutout_mipped";
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
    }

    @Override public ChainDriveBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }

    public ChainDriveBuilder model(String role, String path) {
        modelOverrides.set(role, path);
        return this;
    }

    public ChainDriveBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }

    public ChainDriveBuilder noMaxRPM() {
        maxRpm = null;
        independentRpmLimit = true;
        return this;
    }

    public ChainDriveBuilder maxStress(int value) {
        maxStress = checked("maxStress", value);
        return this;
    }

    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }

    private void validateBlockEntity() {
        if (blockEntityInfo != null)
            throw new IllegalArgumentException("Chain drives own their Create block entity: " + id);
    }

    @Override public Block createObject() {
        validateBlockEntity();
        return new ScriptedChainDriveBlock(this);
    }

    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        ModChainDrives.add(this);
        ScriptedChainDriveBlockEntities.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }

    @Override public ChainDriveBuilder noDrops() {
        super.noDrops();
        dropsDisabled = true;
        disabledLoot = lootTable;
        return this;
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

    public JsonObject shaftModel() { return ChainDriveAssets.shaftModel(this); }

    @Override public void generateAssetJsons(AssetJsonGenerator generator) {
        ChainDriveAssets.generate(this, generator);
    }
}
