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
import net.quiwh.cbjs.content.*;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Script materials and block settings over Create's native transmission behavior. */
public final class TransmissionBuilder extends BlockBuilder {
    public enum Kind {
        CLUTCH("clutch"), GEARSHIFT("gearshift"), SPEED_CONTROLLER("rotation_speed_controller");
        public final String path;
        Kind(String path) { this.path = path; }
    }
    public final Kind kind;
    public final ModelOverrides modelOverrides;
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private Consumer<LootBuilder> disabledLoot;

    public TransmissionBuilder(ResourceLocation id, Kind kind) {
        super(id);
        this.kind = kind;
        modelOverrides = new ModelOverrides(this, Set.of("block", "body"),
                kind == Kind.SPEED_CONTROLLER ? Set.of("shaft", "bracket") : Set.of("powered", "shaft_half"));
        textures.entrySet().clear();
        hardness = kind == Kind.SPEED_CONTROLLER ? 3F : 1.5F;
        resistance = 6;
        soundType = kind == Kind.SPEED_CONTROLLER ? SoundType.METAL : SoundType.STONE;
        notSolid = true;
        opaque = false;
        suffocating = false;
        viewBlocking = false;
        renderType = "cutout_mipped";
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
        tagBlock(ResourceLocation.parse("minecraft:mineable/axe"));
        if (kind == Kind.SPEED_CONTROLLER) tagBlock(ResourceLocation.parse("create:safe_nbt"));
        mapColor(kind == Kind.SPEED_CONTROLLER ? net.minecraft.world.level.material.MapColor.TERRACOTTA_YELLOW
                : net.minecraft.world.level.material.MapColor.PODZOL);
    }
    public TransmissionBuilder replaceTexture(String role, String path) { texture(role, path); return this; }
    @Override public TransmissionBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }
    public TransmissionBuilder model(String role, String path) {
        modelOverrides.set(kind != Kind.SPEED_CONTROLLER && "shaft".equals(role) ? "shaft_half" : role, path);
        return this;
    }
    public TransmissionBuilder maxRpm(int value) { maxRpm = checked("maxRpm", value); independentRpmLimit = true; return this; }
    public TransmissionBuilder noMaxRPM() { maxRpm = null; independentRpmLimit = true; return this; }
    public TransmissionBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }
    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }
    private void validateBlockEntity() {
        if (blockEntityInfo != null) throw new IllegalArgumentException("Transmissions own their Create block entity: " + id);
    }
    @Override public Block createObject() {
        validateBlockEntity();
        return switch (kind) {
            case CLUTCH -> new ScriptedClutchBlock(this);
            case GEARSHIFT -> new ScriptedGearshiftBlock(this);
            case SPEED_CONTROLLER -> new ScriptedSpeedControllerBlock(this);
        };
    }
    @Override public void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        ModTransmissions.add(this);
        ScriptedTransmissionBlockEntities.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }
    @Override public TransmissionBuilder noDrops() {
        super.noDrops(); dropsDisabled = true; disabledLoot = lootTable; return this;
    }
    @Override public void generateDataJsons(DataJsonGenerator generator) {
        JsonObject loot;
        if (lootTable != null && lootTable != disabledLoot) {
            var custom = new LootBuilder(null); custom.type = "minecraft:block"; lootTable.accept(custom); loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(dropsDisabled || itemBuilder == null ? null : id);
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_tables/blocks/" + id.getPath()), loot);
    }
    @Override public void generateAssetJsons(AssetJsonGenerator generator) { TransmissionAssets.generate(this, generator); }
}
