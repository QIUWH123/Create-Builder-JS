package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.block.BlockBuilder;
import dev.latvian.mods.kubejs.block.drop.BlockDropSupplier;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;

/** Addon callbacks retained across KubeJS versions; native KubeJS 7 behavior remains available. */
public abstract class CBJSBlockBuilder extends BlockBuilder {
    public Consumer<PickBlockCallback> pickBlockCallback;
    public Consumer<LootBuilder> lootTable;
    public String model = "";
    public com.google.gson.JsonObject modelJson;
    public com.google.gson.JsonObject blockstateJson;

    protected CBJSBlockBuilder(ResourceLocation id) { super(id); }

    public CBJSBlockBuilder model(String path) {
        model = ModelOverrides.validPath(path);
        if (itemBuilder != null) itemBuilder.parentModel(ResourceLocation.parse(model));
        return this;
    }

    @Override public CBJSBlockBuilder parentModel(ResourceLocation parent) {
        super.parentModel(parent);
        return model(parent.toString());
    }

    @Override public void generateData(dev.latvian.mods.kubejs.generator.KubeDataGenerator generator) {
        if (lootTable == null) { super.generateData(generator); return; }
        var custom = new LootBuilder(null);
        custom.type = "minecraft:block";
        lootTable.accept(custom);
        ResourceLocation table = custom.customId == null ? id : custom.customId;
        generator.json(table.withPath(path -> "loot_table/blocks/" + path), custom.toJson());
    }

    public CBJSBlockBuilder texture(String role, String path) { super.texture(new String[]{role}, path); return this; }
    public CBJSBlockBuilder textureAll(String path) {
        for (String role : new String[]{"all", "north", "south", "east", "west", "up", "down", "particle"})
            textures.put(role, path);
        return this;
    }

    @Override protected dev.latvian.mods.kubejs.block.BlockItemBuilder getOrCreateItemBuilder() {
        if (itemBuilder == null) {
            var item = new CBJSBlockItemBuilder(id);
            item.blockBuilder = this;
            itemBuilder = item;
        }
        return (dev.latvian.mods.kubejs.block.BlockItemBuilder) itemBuilder;
    }

    public CBJSBlockBuilder pickBlock(Consumer<PickBlockCallback> callback) {
        pickBlockCallback = callback;
        return this;
    }

    public CBJSBlockBuilder lootTable(Consumer<LootBuilder> callback) {
        lootTable = callback;
        return this;
    }

    @Override public CBJSBlockBuilder noDrops() {
        super.noDrops();
        lootTable = ignored -> {};
        return this;
    }

    @Override public CBJSBlockBuilder drops(BlockDropSupplier supplier) {
        super.drops(supplier);
        lootTable = supplier == BlockDropSupplier.NO_DROPS ? ignored -> {} : builder -> {
            var table = generateLootTable();
            if (table != null) builder.read(net.minecraft.world.level.storage.loot.LootTable.DIRECT_CODEC
                    .encodeStart(dev.latvian.mods.kubejs.util.RegistryAccessContainer.current.json(), table)
                    .getOrThrow());
        };
        return this;
    }

    @Override public CBJSBlockBuilder tagBlock(ResourceLocation... tags) { super.tagBlock(tags); return this; }
    @Override public CBJSBlockBuilder tagItem(ResourceLocation... tags) { super.tagItem(tags); return this; }
}
