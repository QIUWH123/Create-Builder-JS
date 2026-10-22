package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.Extra;

public final class CBJSEvents {
    public static final EventGroup GROUP = EventGroup.of("CBJSEvents");
    public static final EventHandler KINETIC_DATA =
            GROUP.server("kineticData", () -> KineticDataModifyEventJS.class);
    public static final EventHandler BLAZE_BURNER_FUEL =
            GROUP.server("blazeBurnerFuel", () -> BlazeBurnerFuelEventJS.class);

    public static final EventHandler KINETIC_SPEED_CHANGED =
            GROUP.server("kineticSpeedChanged", () -> KineticSpeedChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler KINETIC_STRESS_CHANGED =
            GROUP.server("kineticStressChanged", () -> KineticStressChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler KINETIC_OVERLOAD =
            GROUP.server("kineticOverload", () -> KineticOverloadEventJS.class).extra(Extra.ID).hasResult();

    public static final EventHandler STORAGE_FLUID_CHANGED =
            GROUP.server("storageFluidChanged", () -> StorageFluidChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler STORAGE_INVENTORY_CHANGED =
            GROUP.server("storageInventoryChanged", () -> StorageInventoryChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler STORAGE_FORMED =
            GROUP.server("storageFormed", () -> StorageFormedEventJS.class).extra(Extra.ID);
    public static final EventHandler STORAGE_RESIZED =
            GROUP.server("storageResized", () -> StorageResizedEventJS.class).extra(Extra.ID);
    public static final EventHandler STORAGE_SPLIT =
            GROUP.server("storageSplit", () -> StorageSplitEventJS.class).extra(Extra.ID);

    public static final EventHandler BELT_ITEM_ENTERED =
            GROUP.server("beltItemEntered", () -> BeltItemTransferEventJS.class).extra(Extra.ID);
    public static final EventHandler BELT_ITEM_EXITED =
            GROUP.server("beltItemExited", () -> BeltItemTransferEventJS.class).extra(Extra.ID);
    public static final EventHandler BELT_ITEM_PROCESSED =
            GROUP.server("beltItemProcessed", () -> BeltItemProcessedEventJS.class).extra(Extra.ID);

    public static final EventHandler GOGGLES_TOOLTIP =
            GROUP.client("gogglesTooltip", () -> GogglesTooltipEventJS.class).extra(Extra.ID);

    public static final EventHandler FLUID_PIPE_FLOW_CHANGED =
            GROUP.server("fluidPipeFlowChanged", () -> FluidPipeFlowChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler FLUID_PIPE_PRESSURE_CHANGED =
            GROUP.server("fluidPipePressureChanged", () -> FluidPipePressureChangedEventJS.class).extra(Extra.ID);
    public static final EventHandler FLUID_PIPE_COLLISION =
            GROUP.server("fluidPipeCollision", () -> FluidPipeCollisionEventJS.class).extra(Extra.ID).hasResult();
    public static final EventHandler FLUID_PIPE_FLOW =
            GROUP.server("fluidPipeFlow", () -> FluidPipeFlowEventJS.class).extra(Extra.ID);

    private CBJSEvents() {
    }
}
