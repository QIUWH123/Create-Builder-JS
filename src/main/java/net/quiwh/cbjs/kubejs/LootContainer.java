package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

/** Legacy fluent loot helpers encoded with the Minecraft 1.21 loot schema. */
public abstract class LootContainer<T extends LootContainer<T>> {
    public abstract T addFunction(JsonObject value);
    public abstract T addCondition(JsonObject value);
    @SuppressWarnings("unchecked") private T self() { return (T) this; }
    private static JsonObject typed(String field, String id) {
        var result = new JsonObject(); result.addProperty(field, "minecraft:" + id); return result;
    }
    private T function(String id, String key, JsonElement value) {
        var result = typed("function", id); if (key != null) result.add(key, value); return addFunction(result);
    }
    public T addConditionalFunction(Consumer<ConditionalFunction> callback) {
        var result = new ConditionalFunction(); callback.accept(result);
        if (result.function != null) {
            result.function.add("conditions", result.conditions.deepCopy()); addFunction(result.function);
        }
        return self();
    }
    public T count(NumberProvider count) { return function("set_count", "count", LootBuilder.number(count)); }
    public T enchantWithLevels(NumberProvider levels, boolean treasure) {
        var function = typed("function", "enchant_with_levels");
        function.add("levels", LootBuilder.number(levels));
        function.addProperty("options", treasure ? "#minecraft:on_random_loot" : "#minecraft:non_treasure");
        return addFunction(function);
    }
    public T enchantRandomly(ResourceLocation... enchantments) {
        var function = typed("function", "enchant_randomly");
        if (enchantments.length > 0) {
            var options = new JsonArray(); for (var enchantment : enchantments) options.add(enchantment.toString());
            function.add("options", options);
        }
        return addFunction(function);
    }
    /** The old NBT field becomes the custom_data item component in 1.21. */
    public T nbt(CompoundTag tag) {
        var function = typed("function", "set_custom_data"); function.addProperty("tag", tag.toString()); return addFunction(function);
    }
    public T furnaceSmelt() { return addFunction(typed("function", "furnace_smelt")); }
    public T lootingEnchant(NumberProvider count, int limit) {
        var function = typed("function", "enchanted_count_increase");
        function.addProperty("enchantment", "minecraft:looting"); function.add("count", LootBuilder.number(count));
        function.addProperty("limit", limit); return addFunction(function);
    }
    public T damage(NumberProvider damage) { return function("set_damage", "damage", LootBuilder.number(damage)); }
    public T name(Component name, LootContext.EntityTarget entity) {
        var function = typed("function", "set_name");
        function.add("name", ComponentSerialization.CODEC.encodeStart(
                dev.latvian.mods.kubejs.util.RegistryAccessContainer.current.json(), name).getOrThrow());
        if (entity != null) function.addProperty("entity", entity.getSerializedName());
        return addFunction(function);
    }
    public T name(Component name) { return name(name, null); }
    public T copyName(CopyNameFunction.NameSource source) {
        var function = typed("function", "copy_name"); function.addProperty("source", source.getSerializedName()); return addFunction(function);
    }
    public T lootTable(ResourceLocation id, long seed) {
        return lootTable(id, seed, ResourceLocation.withDefaultNamespace("chest"));
    }
    public T lootTable(ResourceLocation id, long seed, ResourceLocation blockEntityType) {
        var function = typed("function", "set_loot_table"); function.addProperty("name", id.toString());
        function.addProperty("seed", seed); function.addProperty("type", blockEntityType.toString()); return addFunction(function);
    }
    public T randomChance(double chance) {
        var condition = typed("condition", "random_chance"); condition.addProperty("chance", chance); return addCondition(condition);
    }
    public T randomChanceWithLooting(double chance, double multiplier) {
        var condition = typed("condition", "random_chance_with_enchanted_bonus");
        condition.addProperty("unenchanted_chance", chance); condition.addProperty("enchantment", "minecraft:looting");
        condition.add("enchanted_chance", net.minecraft.world.item.enchantment.LevelBasedValue.CODEC.encodeStart(JsonOps.INSTANCE,
                net.minecraft.world.item.enchantment.LevelBasedValue.perLevel((float) (chance + multiplier), (float) multiplier)).getOrThrow());
        return addCondition(condition);
    }
    public T survivesExplosion() { return addCondition(typed("condition", "survives_explosion")); }
    public T entityProperties(LootContext.EntityTarget entity, JsonObject predicate) {
        var condition = typed("condition", "entity_properties"); condition.addProperty("entity", entity.getSerializedName());
        condition.add("predicate", predicate.deepCopy()); return addCondition(condition);
    }
    public T killedByPlayer() { return addCondition(typed("condition", "killed_by_player")); }
    public T entityScores(LootContext.EntityTarget entity, Map<String, Object> scores) {
        var condition = typed("condition", "entity_scores"); condition.addProperty("entity", entity.getSerializedName());
        condition.add("scores", new com.google.gson.Gson().toJsonTree(scores)); return addCondition(condition);
    }
    public static final class ConditionalFunction extends LootContainer<ConditionalFunction> {
        public JsonObject function;
        public JsonArray conditions = new JsonArray();
        @Override public ConditionalFunction addFunction(JsonObject value) { function = value.deepCopy(); return this; }
        @Override public ConditionalFunction addCondition(JsonObject value) { conditions.add(value.deepCopy()); return this; }
    }
}
