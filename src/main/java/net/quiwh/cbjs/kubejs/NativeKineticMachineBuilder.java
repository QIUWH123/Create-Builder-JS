package net.quiwh.cbjs.kubejs;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.kinetics.GearLimitDefaults;

/** Kinetic limits shared by native generators, meters and machinery. */
public abstract class NativeKineticMachineBuilder extends NativeMachineBuilder {
    public Integer maxRpm;
    public Integer maxStress;
    public boolean independentRpmLimit;

    protected NativeKineticMachineBuilder(ResourceLocation id, String nativeName, Set<String> movingPartNames) {
        super(id, nativeName, movingPartNames);
    }
    public NativeKineticMachineBuilder maxRpm(int value) { maxRpm = nonnegative("maxRpm", value); independentRpmLimit = true; return this; }
    public NativeKineticMachineBuilder noMaxRPM() { maxRpm = null; independentRpmLimit = true; return this; }
    public NativeKineticMachineBuilder maxStress(int value) { maxStress = nonnegative("maxStress", value); return this; }
    private static int nonnegative(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be nonnegative");
        return value;
    }
    protected final void registerKineticDefaults() {
        GearLimitDefaults.register(this, maxRpm, maxStress, independentRpmLimit, null);
    }
}
