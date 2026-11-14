package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptTypePredicate;
import dev.latvian.mods.kubejs.event.EventTargetType;
import dev.latvian.mods.kubejs.event.TargetedEventHandler;
import net.minecraft.resources.ResourceLocation;

public final class CBJSEvents {
    public static final EventGroup GROUP = EventGroup.of("CBJSEvents");
    public static final EventHandler KINETIC_DATA =
            GROUP.server("kineticData", () -> KineticDataModifyEventJS.class).exceptionHandler((event, container, error) -> {
                throw new IllegalArgumentException("Kinetic data callback failed", error);
            });
    public static final EventHandler BURNER_FUEL =
            GROUP.add("burnerFuel", ScriptTypePredicate.STARTUP_OR_SERVER, () -> BurnerFuelEventJS.class)
                    .exceptionHandler((event, container, error) -> {
                        throw new IllegalArgumentException("Burner fuel callback failed", error);
                    });
    public static final EventHandler BLAZE_BURNER_FUEL =
            GROUP.server("blazeBurnerFuel", () -> BlazeBurnerFuelEventJS.class).exceptionHandler((event, container, error) -> {
                throw new IllegalArgumentException("Blaze Burner fuel callback failed", error);
            });

    public static final TargetedEventHandler<ResourceLocation> KINETIC_SPEED_CHANGED =
            GROUP.server("kineticSpeedChanged", () -> KineticSpeedChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> KINETIC_STRESS_CHANGED =
            GROUP.server("kineticStressChanged", () -> KineticStressChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> KINETIC_OVERLOAD =
            GROUP.server("kineticOverload", () -> KineticOverloadEventJS.class).supportsTarget(EventTargetType.ID).hasResult();

    public static final TargetedEventHandler<ResourceLocation> STORAGE_FLUID_CHANGED =
            GROUP.server("storageFluidChanged", () -> StorageFluidChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> STORAGE_INVENTORY_CHANGED =
            GROUP.server("storageInventoryChanged", () -> StorageInventoryChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> STORAGE_FORMED =
            GROUP.server("storageFormed", () -> StorageFormedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> STORAGE_RESIZED =
            GROUP.server("storageResized", () -> StorageResizedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> STORAGE_SPLIT =
            GROUP.server("storageSplit", () -> StorageSplitEventJS.class).supportsTarget(EventTargetType.ID);

    public static final TargetedEventHandler<ResourceLocation> BELT_ITEM_ENTERED =
            GROUP.server("beltItemEntered", () -> BeltItemTransferEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> BELT_ITEM_EXITED =
            GROUP.server("beltItemExited", () -> BeltItemTransferEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> BELT_ITEM_PROCESSED =
            GROUP.server("beltItemProcessed", () -> BeltItemProcessedEventJS.class).supportsTarget(EventTargetType.ID);

    public static final TargetedEventHandler<ResourceLocation> GOGGLES_TOOLTIP =
            GROUP.client("gogglesTooltip", () -> GogglesTooltipEventJS.class).supportsTarget(EventTargetType.ID);

    public static final TargetedEventHandler<ResourceLocation> FLUID_PIPE_FLOW_CHANGED =
            GROUP.server("fluidPipeFlowChanged", () -> FluidPipeFlowChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> FLUID_PIPE_PRESSURE_CHANGED =
            GROUP.server("fluidPipePressureChanged", () -> FluidPipePressureChangedEventJS.class).supportsTarget(EventTargetType.ID);
    public static final TargetedEventHandler<ResourceLocation> FLUID_PIPE_COLLISION =
            GROUP.server("fluidPipeCollision", () -> FluidPipeCollisionEventJS.class).supportsTarget(EventTargetType.ID).hasResult();
    public static final TargetedEventHandler<ResourceLocation> FLUID_PIPE_FLOW =
            GROUP.server("fluidPipeFlow", () -> FluidPipeFlowEventJS.class).supportsTarget(EventTargetType.ID);

    private CBJSEvents() {
    }
}
