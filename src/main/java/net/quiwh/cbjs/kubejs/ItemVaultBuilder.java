package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockItemBuilder;
import dev.latvian.mods.kubejs.generator.AssetJsonGenerator;
import java.util.Set;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.quiwh.cbjs.content.ScriptedItemVaultBlock;
import net.quiwh.cbjs.content.ScriptedItemVaultItem;
import com.simibubi.create.infrastructure.config.AllConfigs;

/** Per-material item storage, horizontal vaults or upright silos. Capacity is slots per block. */
public final class ItemVaultBuilder extends FluidDeviceBuilder {
    public static final Set<String> TEXTURE_ROLES = Set.of("vault_front_small", "vault_front_medium", "vault_front_large",
            "vault_side_small", "vault_side_medium", "vault_side_large", "vault_top_small", "vault_top_medium",
            "vault_top_large", "vault_bottom_small", "vault_bottom_medium", "vault_bottom_large");
    private final boolean vertical;
    private Integer slots;
    private Integer maxSize;
    private Integer maxLength;
    private List<ItemSelector> itemSelectors = List.of();

    public ItemVaultBuilder(ResourceLocation id, boolean vertical) {
        super(id);
        this.vertical = vertical;
        // Item storage is a solid metal cube; undo the shared fluid-device defaults.
        notSolid = false;
        opaque = true;
        fullBlock = true;
        suffocating = true;
        viewBlocking = true;
        redstoneConductor = true;
        soundType = SoundType.NETHERITE_BLOCK;
        renderType = "solid";
        getOrCreateItemBuilder();
    }

    public ItemVaultBuilder(ResourceLocation id) { this(id, false); }
    public ItemVaultBuilder capacity(int value) { slots = positive("capacity", value); return this; }
    public ItemVaultBuilder maxSize(int value) { maxSize = positive("maxSize", value); return this; }
    public ItemVaultBuilder noMaxSize() { maxSize = 0; return this; }
    public ItemVaultBuilder noMaxLength() { maxLength = 0; return this; }
    public ItemVaultBuilder noMaxHeight() { return noMaxLength(); }
    public ItemVaultBuilder maxLength(int value) { maxLength = positive("maxLength", value); return this; }
    public ItemVaultBuilder maxHeight(int value) { return maxLength(value); }
    /** Accept any listed item ID or #item tag. Omit to accept all items. */
    public ItemVaultBuilder itemFilter(String selector) {
        return itemFilter(java.util.Collections.singletonList(selector));
    }
    public ItemVaultBuilder itemFilter(List<String> selectors) {
        if (selectors == null || selectors.isEmpty())
            throw new IllegalArgumentException("itemFilter requires at least one item ID or #item tag");
        itemSelectors = selectors.stream().map(ItemSelector::parse).toList();
        return this;
    }
    public boolean acceptsItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || itemSelectors.isEmpty()) return true;
        for (ItemSelector selector : itemSelectors) if (selector.matches(stack)) return true;
        return false;
    }
    private record ItemSelector(ResourceLocation id, TagKey<Item> tag) {
        static ItemSelector parse(String selector) {
            var parsed = RegistryFilterSelector.parse("itemFilter", selector);
            return new ItemSelector(parsed.id(), parsed.tagged() ? TagKey.create(Registries.ITEM, parsed.id()) : null);
        }
        boolean matches(ItemStack stack) {
            return tag == null ? id.equals(BuiltInRegistries.ITEM.getKey(stack.getItem())) : stack.is(tag);
        }
    }
    public boolean vertical() { return vertical; }
    @Override public ItemVaultBuilder model(String path) {
        throw StorageAssetOverrides.unsupported(this, "model");
    }
    @Override protected void validateBlockEntity() {
        super.validateBlockEntity();
        StorageAssetOverrides.validate(this);
    }
    public int maximumWidth() {
        int safe = net.quiwh.cbjs.content.StorageStructureLimits.maximumWidth(capacityPerBlock());
        return maxSize == null ? Math.min(3, safe) : maxSize == 0 ? safe : Math.min(maxSize, safe);
    }
    public int maximumLength(int width) {
        int safe = net.quiwh.cbjs.content.StorageStructureLimits.maximumLength(capacityPerBlock(), width);
        return maxLength == null ? (int) Math.min((long) width * 3, safe)
                : maxLength == 0 ? safe : Math.min(maxLength, safe);
    }
    public int capacityPerBlock() { return configuredCapacity(); }
    private int configuredCapacity() {
        if (slots != null) return slots;
        var config = AllConfigs.server();
        return config.specification == null || !config.specification.isLoaded() ? 20 : config.logistics.vaultCapacity.get();
    }
    void resolveServerSettings() { validateConfiguration(); }
    public void validateConfiguration() { validateConfiguration(capacityPerBlock()); }
    private void validateConfiguration(int capacity) {
        if (slots == null || maxSize == null || maxLength == null || maxSize == 0 || maxLength == 0) return;
        if (Integer.MAX_VALUE / (long) capacity / maxSize / maxSize < maxLength)
            throw new IllegalArgumentException("Item storage " + id + " exceeds 2147483647 slots at its maximum dimensions");
    }
    public String texturePath(String role) {
        if (!TEXTURE_ROLES.contains(role)) throw new IllegalArgumentException("Unknown item storage texture role: " + role);
        return textureOrDefault(role, "create:block/vault/" + role);
    }
    public String spritePath(String role) {
        if (!TEXTURE_ROLES.contains(role)) throw new IllegalArgumentException("Unknown item storage texture role: " + role);
        return id.getNamespace() + ":block/" + id.getPath() + "/texture/" + role;
    }
    @Override protected BlockItemBuilder getOrCreateItemBuilder() {
        if (!(itemBuilder instanceof StorageItemBuilder)) {
            itemBuilder = new StorageItemBuilder(id); itemBuilder.blockBuilder = this;
        }
        return itemBuilder;
    }
    @Override public Block createObject() { validateBlockEntity(); validateConfiguration(); return new ScriptedItemVaultBlock(this); }
    @Override protected void registerDevice() { validateConfiguration(); ModItemVaults.add(this); }
    @Override public void generateAssetJsons(AssetJsonGenerator generator) { ItemVaultAssets.generate(this, generator); }
    @Override public void generateDataJsons(dev.latvian.mods.kubejs.generator.DataJsonGenerator generator) {
        super.generateDataJsons(generator);
        var builders = ModItemVaults.builders();
        if (builders.contains(this) && builders.get(0) != this) return;
        var tag = new com.google.gson.JsonObject(); tag.addProperty("replace", false);
        var values = new com.google.gson.JsonArray(); values.add("cbjs:scripted_item_storage"); tag.add("values", values);
        generator.json(ResourceLocation.parse("create:tags/create/mounted_item_storage_type/fuel_blacklist"), tag);
    }
    private static int positive(String name, int value) {
        if (value < 1) throw new IllegalArgumentException(name + " must be positive");
        return value;
    }
    private final class StorageItemBuilder extends BlockItemBuilder {
        StorageItemBuilder(ResourceLocation id) { super(id); }
        @Override public Item createObject() { return new ScriptedItemVaultItem(blockBuilder.get(), createItemProperties()); }
    }
}
