package net.quiwh.cbjs.kinetics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.api.KineticData;

/** Startup builders contribute suppliers; blocks/items resolve only after Forge registration. */
public final class GearLimitDefaults {
    private record DefaultRule(Supplier<? extends Block> block, Integer rpm, Integer stress, boolean independentRpmLimit,
                               Supplier<? extends Block> drop) {
    }

    private static final List<DefaultRule> DEFAULTS = new ArrayList<>();

    public static void register(Supplier<? extends Block> block, Integer maxRpm, Integer maxStress,
                                boolean independentRpmLimit, Supplier<? extends Block> dropBlock) {
        Objects.requireNonNull(block, "block supplier");
        if (maxRpm != null && !independentRpmLimit) {
            throw new IllegalArgumentException("An explicit RPM cap must override Create's RPM cap");
        }
        new KineticLimits(maxRpm == null ? null : maxRpm.doubleValue(),
                maxStress == null ? null : maxStress.doubleValue());
        DEFAULTS.add(new DefaultRule(block, maxRpm, maxStress, independentRpmLimit, dropBlock));
    }

    public static Map<Block, KineticData> buildRules() {
        Map<Block, KineticData> result = new LinkedHashMap<>();
        for (DefaultRule rule : DEFAULTS) {
            Block block = Objects.requireNonNull(rule.block().get(), "Unregistered cogwheel default");
            List<ItemStack> drops = rule.drop() == null ? null
                    : List.of(new ItemStack(Objects.requireNonNull(rule.drop().get(), "Unregistered gear drop")));
            KineticData data = new KineticData(block, rule.rpm(), rule.stress(), drops, rule.independentRpmLimit());
            if (result.putIfAbsent(block, data) != null) {
                throw new IllegalStateException("Duplicate builder limits for " + block);
            }
        }
        return Map.copyOf(result);
    }

    private GearLimitDefaults() {
    }
}
