package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.quiwh.cbjs.content.ScriptedShaftBlock;
import net.quiwh.cbjs.content.ScriptedShaftBlockEntities;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Script-created Create shafts, retaining native mechanics and editable block settings. */
public final class ShaftBuilder extends CBJSBlockBuilder {
    public final ModelOverrides modelOverrides = new ModelOverrides(this,
            java.util.Set.of("block", "body", "shaft"), java.util.Set.of());
    public ShaftPoweredBuilder powered;
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;
    private boolean encased = true;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private java.util.function.Consumer<net.quiwh.cbjs.kubejs.LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public ShaftBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    public ShaftBuilder(ResourceLocation id) {
        super(id);
        textures.entrySet().clear();
        hardness = 1.5F;
        resistance = 6;
        soundType = SoundType.METAL;
        notSolid = true;
        opaque = false;
        fullBlock = false;
        suffocating = false;
        viewBlocking = false;
        renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
    }

    @Override public ShaftBuilder model(String path) {
        super.model(ModelOverrides.validPath(path));
        modelOverrides.primarySelected();
        return this;
    }
    public ShaftBuilder model(String role, String path) { modelOverrides.set(role, path); return this; }

    public String shaftTexture() {
        if (textures.containsKey("axis")) return textures.get("axis");
        if (textures.containsKey("shaft")) return textures.get("shaft");
        if (textures.containsKey("all")) return textures.get("all");
        if (textures.containsKey("north")) return textures.get("north");
        return "create:block/axis";
    }
    public ShaftBuilder maxRpm(int value) {
        maxRpm = checked("maxRpm", value);
        independentRpmLimit = true;
        return this;
    }
    public String shaftTopTexture() {
        if (textures.containsKey("axis_top")) return textures.get("axis_top");
        if (textures.containsKey("shaft_top")) return textures.get("shaft_top");
        if (textures.containsKey("axis")) return "create:block/axis_top";
        return shaftTexture().equals("create:block/axis") ? "create:block/axis_top" : shaftTexture();
    }
    public ShaftBuilder noMaxRPM() { maxRpm = null; independentRpmLimit = true; return this; }
    public ShaftBuilder maxStress(int value) { maxStress = checked("maxStress", value); return this; }
    public ShaftBuilder noEncasedVariants() { encased = false; return this; }
    public boolean encasedVariantsEnabled() { return encased; }
    private static int checked(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }
    @Override public ShaftBuilder noDrops() {
        super.noDrops(); dropsDisabled = true; disabledLoot = lootTable; return this;
    }
    @Override public Block createObject() {
        if (blockEntityInfo != null) throw new IllegalArgumentException("Shafts own their Create block entity: " + id);
        return new ScriptedShaftBlock(this);
    }
    @Override public void createAdditionalObjects(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (additionalCreated) return;
        if (blockEntityInfo != null) throw new IllegalArgumentException("Shafts own their Create block entity: " + id);
        additionalCreated = true;
        super.createAdditionalObjects(registry);
        ModShafts.add(this);
        ScriptedShaftBlockEntities.add(this);
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
        powered = new ShaftPoweredBuilder(this);
        if (RegistryObjectStorage.BLOCK.objects.containsKey(powered.id) || RegistryObjectStorage.ITEM.objects.containsKey(powered.id))
            throw new IllegalArgumentException("Generated powered shaft ID already exists: " + powered.id);
        registry.add(net.minecraft.core.registries.Registries.BLOCK, powered); powered.createAdditionalObjects(registry);
        if (encased) {
            for (boolean brass : new boolean[]{false, true}) {
                var companion = new ShaftEncasedBuilder(this, brass);
                if (RegistryObjectStorage.BLOCK.objects.containsKey(companion.id) || RegistryObjectStorage.ITEM.objects.containsKey(companion.id))
                    throw new IllegalArgumentException("Generated shaft companion ID already exists: " + companion.id);
                registry.add(net.minecraft.core.registries.Registries.BLOCK, companion);
                companion.createAdditionalObjects(registry);
            }
        }
    }
    @Override public void generateAssets(KubeAssetGenerator generator) {
        String root = "block/" + id.getPath() + "/block";
        GearAssets.put(generator, id, "models", root, shaftModel());
        GearAssets.put(generator, id, "models", "block/" + id.getPath() + "/belt_pulley", beltPulleyModel());
        GearAssets.put(generator, id, "blockstates", id.getPath(), blockstateJson == null
                ? CasingAssets.axisState(id, true) : blockstateJson);
        if (itemBuilder != null) {
            boolean sharedPrimary = modelOverrides.usesPrimaryItem();
            var item = textureShaftModel(GearAssets.parent(sharedPrimary ? model : "create:block/shaft"), !sharedPrimary);
            GearAssets.put(generator, id, "models/item", id.getPath(), GearAssets.itemOverrides(item,
                    CBJSAssets.itemModel(itemBuilder), sharedPrimary ? null : CBJSAssets.parent(itemBuilder), CBJSAssets.itemTextures(itemBuilder)));
        }
    }
    com.google.gson.JsonObject shaftModel() {
        return textureShaftModel(modelOverrides.primaryModel("create:block/shaft"), !modelOverrides.customPrimary());
    }
    private com.google.gson.JsonObject beltPulleyModel() {
        var model = GearAssets.parent("minecraft:block/block");
        model.addProperty("loader", "neoforge:composite");
        var textures = new com.google.gson.JsonObject();
        textures.addProperty("particle", shaftTexture());
        model.add("textures", textures);
        var children = new com.google.gson.JsonObject();
        children.add("shaft", GearAssets.parent(id.getNamespace() + ":block/" + id.getPath() + "/block"));
        var pulley = GearAssets.load(ResourceLocation.parse("create:block/belt_pulley"));
        pulley.getAsJsonArray("elements").asList().removeIf(element -> element.getAsJsonObject().has("name")
                && element.getAsJsonObject().get("name").getAsString().equals("Axis"));
        children.add("pulley", pulley);
        model.add("children", children);
        return model;
    }
    private com.google.gson.JsonObject textureShaftModel(com.google.gson.JsonObject model, boolean nativeModel) {
        var slots = model.has("textures") ? model.getAsJsonObject("textures") : new com.google.gson.JsonObject();
        if (nativeModel || textures.containsKey("axis") || textures.containsKey("shaft") || textures.containsKey("all") || textures.containsKey("north")) {
            slots.addProperty("0", shaftTexture());
            slots.addProperty("particle", shaftTexture());
        }
        if (nativeModel || textures.containsKey("axis_top") || textures.containsKey("shaft_top") || textures.containsKey("all") || textures.containsKey("north"))
            slots.addProperty("1", shaftTopTexture());
        for (var entry : textures.entrySet())
            if (!java.util.Set.of("axis", "shaft", "axis_top", "shaft_top", "all", "north").contains(entry.getKey()))
                slots.addProperty(entry.getKey(), entry.getValue());
        if (!slots.entrySet().isEmpty()) model.add("textures", slots);
        return model;
    }
    @Override public void generateData(KubeDataGenerator generator) { generateLoot(generator, id); }
    void generateLoot(KubeDataGenerator generator, ResourceLocation target) {
        com.google.gson.JsonObject loot;
        if (lootTable != null && lootTable != disabledLoot) {
            var custom = new net.quiwh.cbjs.kubejs.LootBuilder(null);
            custom.type = "minecraft:block";
            lootTable.accept(custom);
            loot = custom.toJson();
        } else loot = GearAssets.defaultLoot(dropsDisabled || itemBuilder == null ? null : id);
        generator.json(ResourceLocation.fromNamespaceAndPath(target.getNamespace(), "loot_table/blocks/" + target.getPath()), loot);
    }
}
