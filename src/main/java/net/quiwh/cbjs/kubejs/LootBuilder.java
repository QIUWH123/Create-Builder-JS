package net.quiwh.cbjs.kubejs;

import com.google.gson.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.providers.number.*;

/** JSON loot builder retained for existing CBJS block loot callbacks. */
public final class LootBuilder extends LootContainer<LootBuilder> {
    public String type = "minecraft:generic";
    public ResourceLocation customId;
    public JsonArray pools = new JsonArray();
    public JsonArray functions = new JsonArray();
    public JsonArray conditions = new JsonArray();

    public LootBuilder(JsonElement original) { read(original); }
    void read(JsonElement original) {
        if (original == null || !original.isJsonObject()) return;
        JsonObject source = original.getAsJsonObject();
        if (source.has("type")) type = source.get("type").getAsString();
        if (source.has("pools")) pools = source.getAsJsonArray("pools").deepCopy();
        if (source.has("functions")) functions = source.getAsJsonArray("functions").deepCopy();
        if (source.has("conditions")) conditions = source.getAsJsonArray("conditions").deepCopy();
    }
    public JsonObject toJson() {
        var result = new JsonObject();
        if (type != null) result.addProperty("type", type);
        var outputPools = pools.deepCopy();
        // Minecraft has no table-level conditions; apply fluent table filters to every pool.
        if (!conditions.isEmpty()) {
            for (var value : outputPools) {
                var pool = value.getAsJsonObject();
                var poolConditions = pool.has("conditions") ? pool.getAsJsonArray("conditions") : new JsonArray();
                poolConditions.addAll(conditions.deepCopy());
                pool.add("conditions", poolConditions);
            }
        }
        result.add("pools", outputPools);
        if (!functions.isEmpty()) result.add("functions", functions.deepCopy());
        return result;
    }
    public void addPool(Consumer<Pool> callback) { var pool = new Pool(); callback.accept(pool); pools.add(pool.toJson()); }
    public LootBuilder addFunction(JsonObject value) { functions.add(value.deepCopy()); return this; }
    public LootBuilder addCondition(JsonObject value) { conditions.add(value.deepCopy()); return this; }
    public void clearPools() { pools = new JsonArray(); }
    public void clearFunctions() { functions = new JsonArray(); }
    public void clearConditions() { conditions = new JsonArray(); }

    static JsonElement number(NumberProvider value) {
        return NumberProviders.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, value).getOrThrow();
    }
    public static final class Pool extends LootContainer<Pool> {
        public NumberProvider rolls = ConstantValue.exactly(1);
        public NumberProvider bonusRolls = ConstantValue.exactly(0);
        public final JsonArray conditions = new JsonArray(), functions = new JsonArray(), entries = new JsonArray();
        public JsonObject toJson() {
            var result = new JsonObject();
            result.add("rolls", number(rolls)); result.add("bonus_rolls", number(bonusRolls));
            result.add("entries", entries.deepCopy());
            if (!conditions.isEmpty()) result.add("conditions", conditions.deepCopy());
            if (!functions.isEmpty()) result.add("functions", functions.deepCopy());
            return result;
        }
        public void setUniformRolls(float min, float max) { rolls = UniformGenerator.between(min, max); }
        public void setBinomialRolls(int n, float p) { rolls = BinomialDistributionGenerator.binomial(n, p); }
        public Pool addFunction(JsonObject value) { functions.add(value.deepCopy()); return this; }
        public Pool addCondition(JsonObject value) { conditions.add(value.deepCopy()); return this; }
        public Entry addEntry(JsonObject value) { var entry = new Entry(value.deepCopy()); entries.add(entry.json); return entry; }
        public Entry addEmpty(int weight) { return entry("minecraft:empty", null).weight(weight); }
        public Entry addLootTable(ResourceLocation id) {
            var object = new JsonObject(); object.addProperty("type", "minecraft:loot_table");
            object.addProperty("value", id.toString()); return addEntry(object);
        }
        public Entry addItem(ItemStack stack, int weight, NumberProvider count) {
            Entry entry = entry("minecraft:item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).weight(weight);
            var function = new JsonObject(); function.addProperty("function", "minecraft:set_count");
            function.add("count", number(count)); entry.addFunction(function);
            if (!stack.getComponentsPatch().isEmpty()) {
                var components = new JsonObject(); components.addProperty("function", "minecraft:set_components");
                components.add("components", net.minecraft.core.component.DataComponentPatch.CODEC.encodeStart(
                        dev.latvian.mods.kubejs.util.RegistryAccessContainer.current.json(), stack.getComponentsPatch()).getOrThrow());
                entry.addFunction(components);
            }
            return entry;
        }
        public Entry addItem(ItemStack stack, int weight) { return addItem(stack, weight, ConstantValue.exactly(stack.getCount())); }
        public Entry addItem(ItemStack stack) { return addItem(stack, 1); }
        public Entry addTag(String tag, boolean expand) { var entry = entry("minecraft:tag", tag); entry.json.addProperty("expand", expand); return entry; }
        private Entry entry(String type, String name) {
            var json = new JsonObject(); json.addProperty("type", type); if (name != null) json.addProperty("name", name);
            return addEntry(json);
        }
    }
    public static final class Entry extends LootContainer<Entry> {
        public final JsonObject json;
        Entry(JsonObject json) { this.json = json; }
        public Entry weight(int weight) { json.addProperty("weight", weight); return this; }
        public Entry quality(int quality) { json.addProperty("quality", quality); return this; }
        public Entry addFunction(JsonObject value) { array("functions").add(value.deepCopy()); return this; }
        public Entry addCondition(JsonObject value) { array("conditions").add(value.deepCopy()); return this; }
        private JsonArray array(String name) { if (!json.has(name)) json.add(name, new JsonArray()); return json.getAsJsonArray(name); }
    }
}
