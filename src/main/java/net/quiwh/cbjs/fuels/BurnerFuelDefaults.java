package net.quiwh.cbjs.fuels;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent;
import net.quiwh.cbjs.api.event.BlazeBurnerFuelEvent.Rule;

/** Keeps startup fuel declarations unresolved until Forge's item and block registries exist. */
public final class BurnerFuelDefaults {
    private record Default(Supplier<? extends Block> block, Profile profile) { }
    private static final List<Default> DEFAULTS = new ArrayList<>();

    public static void register(Supplier<? extends Block> block, Profile profile) {
        DEFAULTS.add(new Default(Objects.requireNonNull(block, "burner block supplier"), profile.copy()));
    }

    public static Map<Block, Profile> buildProfiles() {
        Map<Block, Profile> result = new LinkedHashMap<>();
        for (Default declaration : DEFAULTS) {
            Block block = Objects.requireNonNull(declaration.block().get(), "Unregistered burner default");
            if (result.putIfAbsent(block, declaration.profile().copy()) != null)
                throw new IllegalStateException("Duplicate burner fuel defaults for " + block);
        }
        return result;
    }

    /** One configuration layer. Nullable flags inherit the lower-priority layer. */
    public static final class Profile {
        private record Fuel(String selector, int ticks, boolean superheated) { }
        private final Map<String, Fuel> fuels = new LinkedHashMap<>();
        private Boolean vanillaFuels;
        private Boolean globalFuels;
        private RuntimeException invalid;

        public Profile addFuel(String selector, double burnTicks) { return add(selector, burnTicks, false); }
        public Profile addSuperFuel(String selector, double burnTicks) { return add(selector, burnTicks, true); }
        public Profile useVanillaFuels(boolean value) { vanillaFuels = value; return this; }
        public Profile useGlobalFuels(boolean value) { globalFuels = value; return this; }

        private Profile add(String selector, double ticks, boolean superheated) {
            try {
                if (!Double.isFinite(ticks) || ticks != Math.rint(ticks) || ticks < 1
                        || ticks > BlazeBurnerFuelEvent.MAX_BURN_TICKS)
                    throw new IllegalArgumentException("Burner burn ticks must be an integer from 1 to " + BlazeBurnerFuelEvent.MAX_BURN_TICKS);
                ResourceLocation id = selectorId(selector);
                String key = (selector.startsWith("#") ? "#" : "") + id;
                if (fuels.putIfAbsent(key, new Fuel(key, (int) ticks, superheated)) != null)
                    throw new IllegalArgumentException("Duplicate burner fuel selector: " + key);
                return this;
            } catch (RuntimeException exception) { invalid = exception; throw exception; }
        }

        @dev.latvian.mods.rhino.util.HideFromJS
        public List<Rule> buildRules() {
            if (invalid != null) throw new IllegalStateException("Burner fuel profile contains an invalid entry", invalid);
            List<Rule> result = new ArrayList<>();
            for (Fuel fuel : fuels.values()) {
                boolean tag = fuel.selector().startsWith("#");
                ResourceLocation id = selectorId(fuel.selector());
                Item item = tag ? null : requireItem(id);
                result.add(new Rule(fuel.selector(), item, tag ? TagKey.create(Registries.ITEM, id) : null,
                        fuel.ticks(), fuel.superheated()));
            }
            return List.copyOf(result);
        }

        @dev.latvian.mods.rhino.util.HideFromJS public Boolean vanillaFuels() { return vanillaFuels; }
        @dev.latvian.mods.rhino.util.HideFromJS public Boolean globalFuels() { return globalFuels; }
        @dev.latvian.mods.rhino.util.HideFromJS
        public Profile copy() {
            Profile copy = new Profile(); copy.fuels.putAll(fuels); copy.vanillaFuels = vanillaFuels;
            copy.globalFuels = globalFuels; copy.invalid = invalid; return copy;
        }
    }

    /** Event registrations resolve immediately; builders deliberately defer this check. */
    public static void validateRegisteredSelector(String selector) {
        ResourceLocation id = selectorId(selector);
        if (!selector.startsWith("#")) requireItem(id);
    }

    private static ResourceLocation selectorId(String selector) {
        boolean tag = selector != null && selector.startsWith("#");
        String value = tag ? selector.substring(1) : selector;
        ResourceLocation id = value == null ? null : ResourceLocation.tryParse(value);
        if (id == null || id.getPath().isEmpty() || !value.contains(":"))
            throw new IllegalArgumentException("Expected a namespaced item ID or #item:tag, got: " + selector);
        return id;
    }

    private static Item requireItem(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!BuiltInRegistries.ITEM.containsKey(id) || item == null || id.equals(ResourceLocation.parse("minecraft:air")))
            throw new IllegalArgumentException("Unknown or empty burner fuel item: " + id);
        return item;
    }

    private BurnerFuelDefaults() { }
}
