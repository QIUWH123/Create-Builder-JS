package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.KubeAssetGenerator;
import dev.latvian.mods.kubejs.generator.KubeDataGenerator;
import dev.latvian.mods.kubejs.registry.RegistryObjectStorage;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlock;
import net.quiwh.cbjs.content.BurnerOptions;
import net.quiwh.cbjs.fuels.BurnerFuelDefaults;
import net.quiwh.cbjs.mixin.BlazeBurnerItemFactory;

/** One native burner family, including its empty cage and fire-lit cage companion. */
public final class BlazeBurnerBuilder extends FluidDeviceBuilder {
    public static final Set<String> PARTS = Set.of("blaze", "blaze_active", "blaze_super", "blaze_super_active",
            "inert", "rods", "rods_2", "super_rods", "super_rods_2", "flame", "goggles", "goggles_small",
            "train_hat", "logistics_hat", "fire", "soul_fire", "empty_item");
    public final ModelOverrides modelOverrides = new ModelOverrides(this, Set.of("block", "body", "burner"), PARTS);
    private final BurnerOptions options;
    public final BurnerFuelDefaults.Profile fuelProfile = new BurnerFuelDefaults.Profile();
    private BlockItemBuilder emptyItem;
    private LitBlazeBurnerBuilder lit;
    private boolean burnerDropsDisabled;
    private boolean explicitLightLevel;
    private java.util.function.Consumer<net.quiwh.cbjs.kubejs.LootBuilder> burnerDisabledLoot;

    public BlazeBurnerBuilder(ResourceLocation id) { this(id, false); }
    public BlazeBurnerBuilder(ResourceLocation id, boolean generic) {
        super(id);
        options = new BurnerOptions(generic);
        soundType = SoundType.NETHERITE_BLOCK;
        renderType = dev.latvian.mods.kubejs.block.BlockRenderType.CUTOUT_MIPPED;
        getOrCreateItemBuilder();
        for (String tag : new String[]{"fan_processing_catalysts/blasting", "fan_processing_catalysts/smoking",
                "fan_transparent", "passive_boiler_heaters"}) tagBlock(ResourceLocation.parse("create:" + tag));
    }
    @dev.latvian.mods.rhino.util.HideFromJS
    public BurnerOptions options() { return options; }
    @dev.latvian.mods.rhino.util.HideFromJS
    public boolean genericBurner() { return options.generic; }
    public BlazeBurnerBuilder maxHeatLevel(String level) { options.maxHeatLevel(level); return this; }
    public BlazeBurnerBuilder addFuel(String itemOrTag, double burnTicks) { fuelProfile.addFuel(itemOrTag, burnTicks); return this; }
    public BlazeBurnerBuilder addSuperFuel(String itemOrTag, double burnTicks) { fuelProfile.addSuperFuel(itemOrTag, burnTicks); return this; }
    public BlazeBurnerBuilder useVanillaFuels(boolean enabled) { fuelProfile.useVanillaFuels(enabled); return this; }
    public BlazeBurnerBuilder useGlobalFuels(boolean enabled) { fuelProfile.useGlobalFuels(enabled); return this; }
    public BlazeBurnerBuilder haveBlaze() { return haveBlaze(true); }
    public BlazeBurnerBuilder haveBlaze(boolean enabled) { options.haveBlaze = enabled; return this; }
    public BlazeBurnerBuilder haveRods() { return haveRods(true); }
    public BlazeBurnerBuilder haveRods(boolean enabled) { options.haveRods = enabled; return this; }
    public BlazeBurnerBuilder haveFlame() { return haveFlame(true); }
    public BlazeBurnerBuilder haveFlame(boolean enabled) { options.haveFlame = enabled; return this; }
    public BlazeBurnerBuilder haveGoggles() { return haveGoggles(true); }
    public BlazeBurnerBuilder haveGoggles(boolean enabled) { options.haveGoggles = enabled; return this; }
    public BlazeBurnerBuilder haveTrainHat() { return haveTrainHat(true); }
    public BlazeBurnerBuilder haveTrainHat(boolean enabled) { options.haveTrainHat = enabled; return this; }
    public BlazeBurnerBuilder haveLogisticsHat() { return haveLogisticsHat(true); }
    public BlazeBurnerBuilder haveLogisticsHat(boolean enabled) { options.haveLogisticsHat = enabled; return this; }
    public BlazeBurnerBuilder haveParticles() { return haveParticles(true); }
    public BlazeBurnerBuilder haveParticles(boolean enabled) { options.haveParticles = enabled; return this; }
    public BlazeBurnerBuilder haveSound() { return haveSound(true); }
    public BlazeBurnerBuilder haveSound(boolean enabled) { options.haveSound = enabled; return this; }
    public BlazeBurnerBuilder canControlTrain() { return canControlTrain(true); }
    public BlazeBurnerBuilder canControlTrain(boolean enabled) { options.canControlTrain = enabled; return this; }
    public BlazeBurnerBuilder canBeStockKeeper() { return canBeStockKeeper(true); }
    public BlazeBurnerBuilder canBeStockKeeper(boolean enabled) { options.canBeStockKeeper = enabled; return this; }
    public BlazeBurnerBuilder canCaptureBlaze() { return canCaptureBlaze(true); }
    public BlazeBurnerBuilder canCaptureBlaze(boolean enabled) { options.canCaptureBlaze = enabled; return this; }
    public BlazeBurnerBuilder canHeatBoiler() { return canHeatBoiler(true); }
    public BlazeBurnerBuilder canHeatBoiler(boolean enabled) { options.canHeatBoiler = enabled; return this; }
    public BlazeBurnerBuilder canHeatBasin() { return canHeatBasin(true); }
    public BlazeBurnerBuilder canHeatBasin(boolean enabled) { options.canHeatBasin = enabled; return this; }
    public BlazeBurnerBuilder canProcessFan() { return canProcessFan(true); }
    public BlazeBurnerBuilder canProcessFan(boolean enabled) { options.canProcessFan = enabled; return this; }
    public BlazeBurnerBuilder canUseCreativeFuel() { return canUseCreativeFuel(true); }
    public BlazeBurnerBuilder canUseCreativeFuel(boolean enabled) { options.canUseCreativeFuel = enabled; return this; }
    public BlazeBurnerBuilder canBeFedByArm() { return canBeFedByArm(true); }
    public BlazeBurnerBuilder canBeFedByArm(boolean enabled) { options.canBeFedByArm = enabled; return this; }
    public ResourceLocation emptyId() { return companionId("empty_"); }
    public ResourceLocation litId() { return companionId("lit_"); }
    private ResourceLocation companionId(String prefix) {
        String path = id.getPath(); int split = path.lastIndexOf('/') + 1;
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, split) + prefix + path.substring(split));
    }
    public Item emptyItem() { return emptyItem == null ? net.minecraft.world.item.Items.AIR : emptyItem.get(); }
    public String texturePath(String role) {
        return textureOrDefault(role, switch (role) {
            case "flame" -> "create:block/blaze_burner_flame";
            case "flame_scroll" -> "create:block/blaze_burner_flame_scroll";
            case "super_flame_scroll" -> "create:block/blaze_burner_flame_superheated_scroll";
            default -> throw new IllegalArgumentException("Unknown burner texture role: " + role);
        });
    }
    public Block litBlock() {
        if (lit == null) throw new IllegalStateException("Burner companions have not been registered: " + id);
        return lit.get();
    }
    public BlazeBurnerBuilder replaceTexture(String role, String path) { texture(role, path); return this; }
    @Override public BlazeBurnerBuilder lightLevel(float value) { super.lightLevel(value); explicitLightLevel = true; return this; }
    @dev.latvian.mods.rhino.util.HideFromJS
    public boolean hasCustomLightLevel() { return explicitLightLevel || lightLevel != 0; }
    @Override public BlazeBurnerBuilder model(String path) { super.model(ModelOverrides.validPath(path)); modelOverrides.primarySelected(); return this; }
    public BlazeBurnerBuilder model(String role, String path) { modelOverrides.set(role, path); return this; }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof CageItemBuilder)) { itemBuilder = new CageItemBuilder(id, true); ((dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder).blockBuilder = this; }
        return (BlockItemBuilder) itemBuilder;
    }
    @Override public Block createObject() { validateBlockEntity(); return new ScriptedBlazeBurnerBlock(this); }
    @Override protected void registerDevice(dev.latvian.mods.kubejs.registry.AdditionalObjectRegistry registry) {
        if (RegistryObjectStorage.ITEM.objects.containsKey(emptyId()) || RegistryObjectStorage.BLOCK.objects.containsKey(litId())
                || RegistryObjectStorage.ITEM.objects.containsKey(litId()))
            throw new IllegalArgumentException("Blaze burner companion ID already registered for " + id);
        emptyItem = new CageItemBuilder(emptyId(), false); emptyItem.blockBuilder = this;
        if (itemBuilder != null) CompanionInheritance.copyItem(itemBuilder, emptyItem);
        registry.add(net.minecraft.core.registries.Registries.ITEM, emptyItem);
        lit = new LitBlazeBurnerBuilder(this);
        registry.add(net.minecraft.core.registries.Registries.BLOCK, lit); lit.createAdditionalObjects(registry);
        BurnerFuelDefaults.register(this::get, fuelProfile);
        ModBlazeBurners.add(this);
    }
    @Override public void generateAssets(KubeAssetGenerator generator) { BlazeBurnerAssets.generate(this, generator); }
    @Override public BlazeBurnerBuilder noDrops() {
        super.noDrops(); burnerDropsDisabled = true; burnerDisabledLoot = lootTable; return this;
    }
    JsonObject familyLoot(boolean lit) {
        if (lootTable != null && lootTable != burnerDisabledLoot) {
            var custom = new net.quiwh.cbjs.kubejs.LootBuilder(null);
            custom.type = "minecraft:block"; lootTable.accept(custom); return custom.toJson();
        }
        return burnerDropsDisabled ? GearAssets.defaultLoot(null) : lit
                ? GearAssets.defaultLoot(emptyId()) : BlazeBurnerAssets.loot(this);
    }
    @Override public void generateData(KubeDataGenerator generator) {
        generator.json(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "loot_table/blocks/" + id.getPath()), familyLoot(false));
    }
    private final class CageItemBuilder extends CBJSBlockItemBuilder {
        private final boolean filled;
        CageItemBuilder(ResourceLocation id, boolean filled) { super(id); this.filled = filled; }
        @Override public Item createObject() { return BlazeBurnerItemFactory.cbjs$create(blockBuilder.get(), createItemProperties(), filled); }
        @Override public void generateAssets(KubeAssetGenerator generator) {
            // The family generator emits the material-matched filled and empty models together.
        }
    }
}
