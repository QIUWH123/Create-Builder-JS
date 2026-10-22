package net.quiwh.cbjs.api;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.kinetics.KineticLimits;

/** A material limit and optional replacement drops. RPM defaults to Create unless explicitly overridden. */
public record KineticData(Block block, Integer maxSpeed, Integer maxNetworkStress, List<ItemStack> list,
                          boolean independentRpmLimit) {
    /** Java rules with explicit limits retain their independent RPM behavior. */
    public KineticData(Block block, Integer maxSpeed, Integer maxNetworkStress, List<ItemStack> list) {
        this(block, maxSpeed, maxNetworkStress, list, true);
    }

    public KineticData {
        Objects.requireNonNull(block, "block");
        if (maxSpeed != null && !independentRpmLimit) {
            throw new IllegalArgumentException("An explicit RPM cap must override Create's RPM cap");
        }
        new KineticLimits(maxSpeed == null ? null : maxSpeed.doubleValue(),
                maxNetworkStress == null ? null : maxNetworkStress.doubleValue());
        if (list != null) {
            list = list.stream().map(stack -> Objects.requireNonNull(stack, "drop stack").copy()).toList();
        }
    }

    @Override
    public List<ItemStack> list() {
        return list == null ? null : list.stream().map(ItemStack::copy).toList();
    }

    public KineticLimits limits() {
        return new KineticLimits(maxSpeed == null ? null : maxSpeed.doubleValue(),
                maxNetworkStress == null ? null : maxNetworkStress.doubleValue());
    }

    /** The displayed limit follows the live Create configuration in default RPM mode. */
    public Integer effectiveMaxRpm(int createMaxRpm) {
        return independentRpmLimit ? maxSpeed : Integer.valueOf(createMaxRpm);
    }

    public boolean isExceeded(double rpm, double networkStress) {
        return KineticLimits.isExceeded(rpm, networkStress,
                maxSpeed == null ? Double.POSITIVE_INFINITY : maxSpeed.doubleValue(),
                maxNetworkStress == null ? Double.POSITIVE_INFINITY : maxNetworkStress.doubleValue());
    }
}
