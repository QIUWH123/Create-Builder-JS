package net.quiwh.cbjs.api.event;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.registries.ForgeRegistries;

/** Builds one replacement set of custom fuels per server data reload. */
public final class BlazeBurnerFuelEvent extends Event {
    // Create adds up to 9999 existing ticks before clamping normal fuel overflow.
    public static final int MAX_BURN_TICKS = Integer.MAX_VALUE - 10000;
    private final Map<String, Rule> rules = new LinkedHashMap<>();
    private RuntimeException invalidRule;

    public BlazeBurnerFuelEvent addFuel(String selector, double burnTicks) {
        return add(selector, burnTicks, false);
    }

    public BlazeBurnerFuelEvent addSuperFuel(String selector, double burnTicks) {
        return add(selector, burnTicks, true);
    }

    private BlazeBurnerFuelEvent add(String selector, double burnTicks, boolean superheated) {
        try {
            if (!Double.isFinite(burnTicks) || burnTicks != Math.rint(burnTicks)
                    || burnTicks < 1 || burnTicks > MAX_BURN_TICKS) {
                throw new IllegalArgumentException("Blaze Burner burn ticks must be an integer from 1 to " + MAX_BURN_TICKS);
            }
            boolean tag = selector != null && selector.startsWith("#");
            String value = tag ? selector.substring(1) : selector;
            ResourceLocation id = value == null ? null : ResourceLocation.tryParse(value);
            if (id == null || id.getPath().isEmpty() || !value.contains(":")) {
                throw new IllegalArgumentException("Expected a namespaced item ID or #item:tag, got: " + selector);
            }
            Item item = tag ? null : ForgeRegistries.ITEMS.getValue(id);
            if (!tag && (!ForgeRegistries.ITEMS.containsKey(id) || item == null
                    || id.equals(ResourceLocation.parse("minecraft:air")))) {
                throw new IllegalArgumentException("Unknown or empty Blaze Burner fuel item: " + id);
            }
            String key = (tag ? "#" : "") + id;
            if (rules.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate Blaze Burner fuel selector: " + key);
            }
            rules.put(key, new Rule(key, item, tag ? TagKey.create(Registries.ITEM, id) : null,
                    (int) burnTicks, superheated));
            return this;
        } catch (RuntimeException exception) {
            // KubeJS may swallow callback errors; prevent publishing a partially valid set.
            invalidRule = exception;
            throw exception;
        }
    }

    public List<Rule> buildRules() {
        if (invalidRule != null) {
            throw new IllegalStateException("Blaze Burner fuel rules contain an invalid entry", invalidRule);
        }
        return List.copyOf(rules.values());
    }

    /** Tags test the actual inserted stack and retain current datapack holder membership. */
    public record Rule(String selector, Item item, TagKey<Item> tag, int burnTicks, boolean superheated) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && (tag == null ? stack.is(item) : stack.is(tag));
        }
    }
}
