package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.generator.DataJsonGenerator;
import dev.latvian.mods.kubejs.loot.LootBuilder;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.SoundType;

/** Common script metadata for native Create fluid devices with no generated companion blocks. */
public abstract class FluidDeviceBuilder extends BlockBuilder {
    String allTexture;
    private boolean additionalCreated;
    private boolean dropsDisabled;
    private Consumer<LootBuilder> disabledLoot;

    /** Replace one texture slot while preserving the family's default geometry. */
    public FluidDeviceBuilder replaceTexture(String role, String path) {
        texture(role, path);
        return this;
    }

    protected FluidDeviceBuilder(ResourceLocation id) {
        super(id);
        // BlockBuilder calls textureAll virtually during construction; native textures are the defaults here.
        textures.entrySet().clear();
        allTexture = null;
        hardness = 3;
        resistance = 6;
        soundType = SoundType.COPPER;
        notSolid = true;
        opaque = false;
        fullBlock = false;
        suffocating = false;
        viewBlocking = false;
        redstoneConductor = false;
        requiresTool = true;
        tagBlock(ResourceLocation.parse("minecraft:mineable/pickaxe"));
    }

    @Override public FluidDeviceBuilder textureAll(String texture) {
        super.textureAll(texture);
        allTexture = texture;
        return this;
    }

    protected String textureOrDefault(String role, String fallback) {
        if (textures.has(role)) return textures.get(role).getAsString();
        return allTexture == null ? fallback : allTexture;
    }

    protected void validateBlockEntity() {
        if (blockEntityInfo != null)
            throw new IllegalArgumentException("Fluid devices own their Create block entity: " + id);
    }

    @Override public final void createAdditionalObjects() {
        if (additionalCreated) return;
        validateBlockEntity();
        additionalCreated = true;
        super.createAdditionalObjects();
        registerDevice();
    }

    protected abstract void registerDevice();

    @Override public FluidDeviceBuilder noDrops() {
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
}
