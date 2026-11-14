package net.quiwh.cbjs.api.event;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.api.KineticData;

/** Posted on the Forge bus after server scripts load, once per server data reload. */
public final class KineticDataModifyEvent extends Event {
    private final Map<Block, KineticData> rules;
    private final Set<Block> explicitRules = new HashSet<>();
    private RuntimeException invalidRule;

    public KineticDataModifyEvent() {
        this(Map.of());
    }

    public KineticDataModifyEvent(Map<Block, KineticData> defaults) {
        rules = new LinkedHashMap<>(defaults);
    }

    /** Modify one block per reload, preserving any startup defaults for unspecified settings. */
    public KineticDataModifier modifyKineticData(String blockId) {
        try {
            ResourceLocation id = requireId(blockId);
            if (!BuiltInRegistries.BLOCK.containsKey(id)) {
                throw new IllegalArgumentException("Unknown kinetic block: " + id);
            }
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (!explicitRules.add(block)) {
                throw new IllegalArgumentException("Duplicate kinetic rule: " + id);
            }
            rules.putIfAbsent(block, new KineticData(block, null, null, null, false));
            return new KineticDataModifier(block);
        } catch (RuntimeException exception) {
            invalidRule = exception;
            throw exception;
        }
    }

    /**
     * Java-only replacement rules: null drops use normal loot, an empty list suppresses drops,
     * and supplied stacks are copied. Null RPM explicitly removes Create's cap.
     */
    public void addKineticDataWithDrops(Block block, Integer maxSpeed, Integer maxNetworkStress, List<ItemStack> drops) {
        try {
            KineticData data = new KineticData(block, maxSpeed, maxNetworkStress, drops);
            if (!explicitRules.add(block)) {
                throw new IllegalArgumentException("Duplicate kinetic rule: " + BuiltInRegistries.BLOCK.getKey(block));
            }
            rules.put(block, data);
        } catch (RuntimeException exception) {
            // KubeJS may catch handler exceptions; keep this event invalid even if it does.
            invalidRule = exception;
            throw exception;
        }
    }

    public Map<Block, KineticData> buildRules() {
        if (invalidRule != null) {
            throw new IllegalStateException("Kinetic rules contain an invalid entry", invalidRule);
        }
        return Map.copyOf(rules);
    }

    /** Fluent scripting API. Each setter changes only its own limit. */
    public final class KineticDataModifier {
        private final Block block;

        private KineticDataModifier(Block block) {
            this.block = block;
        }

        public KineticDataModifier maxRpm(int value) {
            return replace(value, rules.get(block).maxNetworkStress(), true);
        }

        public KineticDataModifier noMaxRPM() {
            return replace(null, rules.get(block).maxNetworkStress(), true);
        }

        public KineticDataModifier maxStress(int value) {
            KineticData previous = rules.get(block);
            return replace(previous.maxSpeed(), value, previous.independentRpmLimit());
        }

        /** Base consumer stress in SU per RPM, before native machine-specific factors. */
        public KineticDataModifier stressImpact(double value) {
            return replaceStress(value, rules.get(block).stressCapacity());
        }

        /** Base generator capacity in SU per RPM, before native generator-specific factors. */
        public KineticDataModifier stressCapacity(double value) {
            return replaceStress(rules.get(block).stressImpact(), value);
        }

        private KineticDataModifier replaceStress(Double impact, Double capacity) {
            try {
                KineticData previous = rules.get(block);
                rules.put(block, new KineticData(block, previous.maxSpeed(), previous.maxNetworkStress(),
                        previous.list(), previous.independentRpmLimit(), impact, capacity));
                return this;
            } catch (RuntimeException exception) {
                invalidRule = exception;
                throw exception;
            }
        }

        private KineticDataModifier replace(Integer maxRpm, Integer maxStress, boolean independentRpmLimit) {
            try {
                KineticData previous = rules.get(block);
                rules.put(block, new KineticData(block, maxRpm, maxStress, previous.list(), independentRpmLimit,
                        previous.stressImpact(), previous.stressCapacity()));
                return this;
            } catch (RuntimeException exception) {
                invalidRule = exception;
                throw exception;
            }
        }
    }

    private static ResourceLocation requireId(String value) {
        ResourceLocation id = value == null ? null : ResourceLocation.tryParse(value);
        if (id == null || !value.contains(":")) {
            throw new IllegalArgumentException("Expected a namespaced registry ID, got: " + value);
        }
        return id;
    }
}
