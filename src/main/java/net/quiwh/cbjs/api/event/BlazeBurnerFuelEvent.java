package net.quiwh.cbjs.api.event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.fuels.BurnerFuelDefaults;
import net.quiwh.cbjs.fuels.BurnerFuelDefaults.Profile;

/** Builds atomic global and per-burner fuel layers, retaining the original Java event identity. */
public final class BlazeBurnerFuelEvent extends Event {
    // Create adds up to 9999 existing ticks before clamping normal fuel overflow.
    public static final int MAX_BURN_TICKS = Integer.MAX_VALUE - 10000;
    private final List<Profile> global = new ArrayList<>();
    private final Map<Block, List<Profile>> profiles = new LinkedHashMap<>();
    private final Set<Block> explicitRules = new HashSet<>();
    private Profile currentGlobal;
    private RuntimeException invalidRule;

    public BlazeBurnerFuelEvent() { this(Map.of()); }
    public BlazeBurnerFuelEvent(Map<Block, Profile> defaults) {
        defaults.forEach((block, profile) -> profiles.put(block, new ArrayList<>(List.of(profile.copy()))));
        beginLayer();
    }

    /** Start the next priority layer; a server reload replays startup before server callbacks. */
    @dev.latvian.mods.rhino.util.HideFromJS
    public void beginLayer() { explicitRules.clear(); currentGlobal = new Profile(); global.add(currentGlobal); }

    public BlazeBurnerFuelEvent addFuel(String selector, double ticks) { add(currentGlobal, selector, ticks, false); return this; }
    public BlazeBurnerFuelEvent addSuperFuel(String selector, double ticks) { add(currentGlobal, selector, ticks, true); return this; }

    public BurnerFuelModifier modifyBurner(String blockId) {
        try {
            ResourceLocation id = blockId == null ? null : ResourceLocation.tryParse(blockId);
            if (id == null || !blockId.contains(":") || !BuiltInRegistries.BLOCK.containsKey(id))
                throw new IllegalArgumentException("Unknown or invalid burner block: " + blockId);
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (!explicitRules.add(block)) throw new IllegalArgumentException("Duplicate burner fuel profile: " + id);
            Profile profile = new Profile(); profiles.computeIfAbsent(block, unused -> new ArrayList<>()).add(profile);
            return new BurnerFuelModifier(profile);
        } catch (RuntimeException exception) { invalidRule = exception; throw exception; }
    }

    private void add(Profile profile, String selector, double ticks, boolean superheated) {
        try {
            BurnerFuelDefaults.validateRegisteredSelector(selector);
            if (superheated) profile.addSuperFuel(selector, ticks); else profile.addFuel(selector, ticks);
        } catch (RuntimeException exception) { invalidRule = exception; throw exception; }
    }

    /** Compatibility projection of shared rules, in registration order. */
    public List<Rule> buildRules() {
        if (invalidRule != null) throw new IllegalStateException("Burner fuel rules contain an invalid entry", invalidRule);
        return global.stream().flatMap(profile -> profile.buildRules().stream()).toList();
    }

    @dev.latvian.mods.rhino.util.HideFromJS
    public Configuration buildConfiguration() {
        buildRules();
        Map<Block, List<Layer>> result = new LinkedHashMap<>();
        profiles.forEach((block, layers) -> result.put(block, layers.stream().map(Layer::of).toList()));
        return new Configuration(global.stream().map(Layer::of).toList(), Map.copyOf(result));
    }

    public final class BurnerFuelModifier {
        private final Profile profile;
        private BurnerFuelModifier(Profile profile) { this.profile = profile; }
        public BurnerFuelModifier addFuel(String selector, double ticks) { add(profile, selector, ticks, false); return this; }
        public BurnerFuelModifier addSuperFuel(String selector, double ticks) { add(profile, selector, ticks, true); return this; }
        public BurnerFuelModifier useVanillaFuels(boolean value) { profile.useVanillaFuels(value); return this; }
        public BurnerFuelModifier useGlobalFuels(boolean value) { profile.useGlobalFuels(value); return this; }
    }

    public record Configuration(List<Layer> global, Map<Block, List<Layer>> profiles) { }
    public record Layer(List<Rule> rules, Boolean vanillaFuels, Boolean globalFuels) {
        private static Layer of(Profile profile) { return new Layer(profile.buildRules(), profile.vanillaFuels(), profile.globalFuels()); }
    }

    /** Tags retain live datapack holder membership rather than cached member lists. */
    public record Rule(String selector, Item item, TagKey<Item> tag, int burnTicks, boolean superheated) {
        public boolean matches(ItemStack stack) { return !stack.isEmpty() && (tag == null ? stack.is(item) : stack.is(tag)); }
    }
}
