package net.quiwh.cbjs.kubejs;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.storage.StorageSnapshot;

/** Net executed fluid change during a server tick; simulation never reaches this event. */
public final class StorageFluidChangedEventJS extends StorageEventJS {
    private final FluidStack previous;
    private final FluidStack current;
    public StorageFluidChangedEventJS(StorageSnapshot controller, FluidStack previous, FluidStack current) {
        super(controller, controller.dimensions());
        this.previous = previous.copy();
        this.current = current.copy();
    }
    public FluidStack getOldFluid() { return previous.copy(); }
    public FluidStack getFluid() { return current.copy(); }
    public FluidStack getNewFluid() { return getFluid(); }
    public String getOldFluidId() { return fluidId(previous); }
    public String getFluidId() { return fluidId(current); }
    public String getNewFluidId() { return getFluidId(); }
    public int getOldAmount() { return previous.getAmount(); }
    public int getAmount() { return current.getAmount(); }
    public int getNewAmount() { return getAmount(); }
    FluidStack filterOldFluid() { return previous; }
    FluidStack filterFluid() { return current; }
    private static String fluidId(FluidStack fluid) {
        return fluid.isEmpty() ? "minecraft:empty" : ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString();
    }
}
