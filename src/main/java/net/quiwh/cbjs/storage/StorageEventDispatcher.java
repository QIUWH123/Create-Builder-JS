package net.quiwh.cbjs.storage;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity;
import net.quiwh.cbjs.content.ScriptedItemVaultBlockEntity;
import net.quiwh.cbjs.kubejs.*;

/** Mutation-driven, one-tick queue. No loaded-entity polling or full inventory snapshots. */
public final class StorageEventDispatcher {
    private static volatile Batch pending;
    private static volatile long generation;

    public static boolean hasFluidListeners(ScriptedFluidTankBlockEntity entity) {
        return CBJSEvents.STORAGE_FLUID_CHANGED.hasListeners() && observable(entity)
                && entity.isController() && matches(CBJSEvents.STORAGE_FLUID_CHANGED, entity);
    }

    public static boolean hasInventoryListeners(ScriptedItemVaultBlockEntity entity) {
        return CBJSEvents.STORAGE_INVENTORY_CHANGED.hasListeners() && observable(entity)
                && matches(CBJSEvents.STORAGE_INVENTORY_CHANGED, entity);
    }

    /** Once queued, the first old value already belongs to this active script batch. */
    public static boolean needsFluidSnapshot(ScriptedFluidTankBlockEntity entity) {
        if (!hasFluidListeners(entity)) return false;
        Batch current = pending;
        return current == null || !current.active() || !current.fluids.containsKey(entity);
    }

    public static boolean needsInventorySnapshot(ScriptedItemVaultBlockEntity member, int slot) {
        if (!hasInventoryListeners(member)) return false;
        var root = member.getControllerBE();
        if (root == null || !isCurrent(root)) return false;
        Batch current = pending;
        if (current == null || !current.active()) return true;
        var changes = current.inventories.get(new Slot(member, slot));
        return changes == null || !changes.containsKey(root);
    }

    public static void fluidChanged(ScriptedFluidTankBlockEntity entity, FluidStack previous) {
        if (!hasFluidListeners(entity) || previous.isFluidStackIdentical(entity.getTankInventory().getFluid())) return;
        batch().fluids.putIfAbsent(entity, previous);
    }

    public static void inventoryChanged(ScriptedItemVaultBlockEntity member, int slot, ItemStack previous) {
        if (!hasInventoryListeners(member) || ItemStack.matches(previous, member.getInventoryOfBlock().getStackInSlot(slot))) return;
        var root = member.getControllerBE();
        if (root == null || !isCurrent(root)) return;
        batch().inventories.computeIfAbsent(new Slot(member, slot), ignored -> new LinkedHashMap<>())
                .putIfAbsent(root, previous);
    }

    /** Native splitting shrinks a live FluidStack before calling any tank mutator. */
    public static void beforeFluidRedistribution(BlockEntity member) {
        if (!CBJSEvents.STORAGE_FLUID_CHANGED.hasListeners()
                || !(member instanceof ScriptedFluidTankBlockEntity tank)
                || !(tank.getControllerBE() instanceof ScriptedFluidTankBlockEntity root)
                || !hasFluidListeners(root) || root.getWidth() == 1 && root.getHeight() == 1) return;
        structureChanging(root);
        batch().fluids.computeIfAbsent(root, ignored -> root.getTankInventory().getFluid().copy());
    }

    /** Called before native geometry/controller changes, including native temporary splits. */
    public static void structureChanging(BlockEntity entity) {
        if (!tracksStructure() || !observable(entity) || !matchesAny(entity)) return;
        var multi = (IMultiBlockEntityContainer) entity;
        var current = batch();
        BlockEntity root = multi.isController() ? entity : multi.getControllerBE();
        if (root == null) return;
        current.structures.computeIfAbsent(root, ignored -> new Structure(StorageSnapshot.of(root)));
    }

    /** Chunk unloading retains the old state; physical replacement has already changed it. */
    public static void removed(BlockEntity entity) {
        if (!tracksStructure() || entity.getLevel() == null || entity.getLevel().isClientSide
                || entity.getLevel().getBlockState(entity.getBlockPos()).is(entity.getBlockState().getBlock())) return;
        structureChanging(entity);
        if (pending != null && ((IMultiBlockEntityContainer) entity).isController()) {
            var structure = pending.structures.get(entity);
            if (structure != null) structure.removed = true;
        }
    }

    /** Native KubeJS clear/reload hook; retain neither callbacks nor pre-reload queued changes. */
    public static void invalidate(EventHandler handler, ScriptType type) {
        if (type == ScriptType.SERVER && (handler == CBJSEvents.STORAGE_FLUID_CHANGED
                || handler == CBJSEvents.STORAGE_INVENTORY_CHANGED || handler == CBJSEvents.STORAGE_FORMED
                || handler == CBJSEvents.STORAGE_RESIZED || handler == CBJSEvents.STORAGE_SPLIT)) clear();
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) flush();
    }
    public static void onServerStopped(ServerStoppedEvent event) { clear(); }
    public static void clear() { pending = null; generation++; }

    /** Detaches the batch before callbacks so script mutations belong to the next report. */
    public static void flush() {
        Batch current = pending;
        pending = null;
        if (current == null || !current.active()) return;

        // Snapshot every completed payload before callbacks can mutate another queued controller.
        List<Runnable> deliveries = new ArrayList<>();
        for (var entry : current.structures.entrySet()) {
            BlockEntity entity = entry.getKey();
            Structure structure = entry.getValue();
            StorageDimensions previous = structure.previous.dimensions();
            StorageDimensions next;
            if (isCurrent(entity)) {
                var multi = (IMultiBlockEntityContainer) entity;
                if (!multi.isController()) continue; // An old controller was absorbed by a merge.
                next = StorageDimensions.of(multi);
            } else if (structure.removed) next = previous.empty();
            else continue;
            if (previous.equals(next) || previous.blockCount() <= 1 && next.blockCount() <= 1) continue;
            EventHandler handler;
            StorageStructureEventJS event;
            if (previous.blockCount() <= 1 && next.blockCount() > 1) {
                handler = CBJSEvents.STORAGE_FORMED;
                if (!matches(handler, structure.previous)) continue;
                event = new StorageFormedEventJS(structure.previous, next);
            } else if (previous.blockCount() > 1 && next.blockCount() < previous.blockCount()) {
                handler = CBJSEvents.STORAGE_SPLIT;
                if (!matches(handler, structure.previous)) continue;
                event = new StorageSplitEventJS(structure.previous, next);
            } else {
                handler = CBJSEvents.STORAGE_RESIZED;
                if (!matches(handler, structure.previous)) continue;
                event = new StorageResizedEventJS(structure.previous, next);
            }
            deliveries.add(() -> {
                if (current.active() && (structure.removed || isCurrent(entity)))
                    handler.post(event, ForgeRegistries.BLOCKS.getKey(structure.previous.state().getBlock()));
            });
        }
        for (var entry : current.fluids.entrySet()) {
            var entity = entry.getKey();
            if (!isCurrent(entity) || !entity.isController() || structureChanged(current, entity)
                    || !matches(CBJSEvents.STORAGE_FLUID_CHANGED, entity)) continue;
            FluidStack next = entity.getTankInventory().getFluid();
            if (entry.getValue().isFluidStackIdentical(next)) continue;
            var event = new StorageFluidChangedEventJS(StorageSnapshot.of(entity), entry.getValue(), next);
            deliveries.add(() -> {
                if (current.active() && isCurrent(entity) && entity.isController())
                    CBJSEvents.STORAGE_FLUID_CHANGED.post(event, ForgeRegistries.BLOCKS.getKey(entity.getBlockState().getBlock()));
            });
        }
        Map<ScriptedItemVaultBlockEntity, List<StorageSlotChange>> inventoryChanges = new LinkedHashMap<>();
        for (var entry : current.inventories.entrySet()) {
            Slot slot = entry.getKey();
            if (!isCurrent(slot.member) || slot.index >= slot.member.getInventoryOfBlock().getSlots()) continue;
            var root = slot.member.getControllerBE();
            if (root == null || !isCurrent(root) || !root.isController() || structureChanged(current, root)
                    || !matches(CBJSEvents.STORAGE_INVENTORY_CHANGED, root)) continue;
            ItemStack previous = null;
            for (var origin : entry.getValue().entrySet()) {
                // Keep the existing suppression for mutations made before a permanent resize/split.
                if (isCurrent(origin.getKey()) && (!origin.getKey().isController()
                        || !structureChanged(current, origin.getKey()))) {
                    previous = origin.getValue();
                    break;
                }
            }
            if (previous == null) continue;
            ItemStack next = slot.member.getInventoryOfBlock().getStackInSlot(slot.index);
            if (!ItemStack.matches(previous, next))
                inventoryChanges.computeIfAbsent(root, ignored -> new ArrayList<>())
                        .add(new StorageSlotChange(slot.member.getBlockPos(), slot.index, previous, next));
        }
        for (var entry : inventoryChanges.entrySet()) {
            var root = entry.getKey();
            var event = new StorageInventoryChangedEventJS(StorageSnapshot.of(root), entry.getValue());
            deliveries.add(() -> {
                if (current.active() && isCurrent(root) && root.isController())
                    CBJSEvents.STORAGE_INVENTORY_CHANGED.post(event, ForgeRegistries.BLOCKS.getKey(root.getBlockState().getBlock()));
            });
        }
        deliveries.forEach(Runnable::run);
    }

    private static boolean structureChanged(Batch current, BlockEntity root) {
        Structure structure = current.structures.get(root);
        return structure != null && !structure.previous.dimensions()
                .equals(StorageDimensions.of((IMultiBlockEntityContainer) root));
    }

    private static boolean tracksStructure() {
        return CBJSEvents.STORAGE_FORMED.hasListeners() || CBJSEvents.STORAGE_RESIZED.hasListeners()
                || CBJSEvents.STORAGE_SPLIT.hasListeners() || CBJSEvents.STORAGE_FLUID_CHANGED.hasListeners()
                || CBJSEvents.STORAGE_INVENTORY_CHANGED.hasListeners();
    }
    private static boolean matchesAny(BlockEntity entity) {
        return matches(CBJSEvents.STORAGE_FORMED, entity) || matches(CBJSEvents.STORAGE_RESIZED, entity)
                || matches(CBJSEvents.STORAGE_SPLIT, entity) || matches(CBJSEvents.STORAGE_FLUID_CHANGED, entity)
                || matches(CBJSEvents.STORAGE_INVENTORY_CHANGED, entity);
    }
    private static boolean matches(EventHandler handler, BlockEntity entity) {
        return StorageContentFilters.hasListeners(handler, entity.getBlockState().getBlock());
    }
    private static boolean matches(EventHandler handler, StorageSnapshot snapshot) {
        return StorageContentFilters.hasListeners(handler, snapshot.state().getBlock());
    }
    private static boolean observable(BlockEntity entity) {
        return entity != null && entity.getLevel() != null && !entity.getLevel().isClientSide
                && (!(entity instanceof SmartBlockEntity smart) || !smart.isVirtual());
    }
    private static boolean isCurrent(BlockEntity entity) {
        return observable(entity) && !entity.isRemoved() && entity.getLevel().hasChunkAt(entity.getBlockPos())
                && entity.getLevel().getBlockEntity(entity.getBlockPos()) == entity;
    }
    private static Object scope() {
        return ServerScriptManager.instance == null ? null : ServerScriptManager.instance.topLevelScope;
    }
    private static Batch batch() {
        if (pending == null || !pending.active()) pending = new Batch(scope());
        return pending;
    }
    private record Slot(ScriptedItemVaultBlockEntity member, int index) { }
    private static final class Structure {
        final StorageSnapshot previous;
        boolean removed;
        Structure(StorageSnapshot previous) { this.previous = previous; }
    }
    private static final class Batch {
        final Object scope;
        final long generation = StorageEventDispatcher.generation;
        final Map<BlockEntity, Structure> structures = new LinkedHashMap<>();
        final Map<ScriptedFluidTankBlockEntity, FluidStack> fluids = new LinkedHashMap<>();
        // Preserve mutation order for each local slot even while its controller temporarily changes.
        final Map<Slot, Map<ScriptedItemVaultBlockEntity, ItemStack>> inventories = new LinkedHashMap<>();
        Batch(Object scope) { this.scope = scope; }
        boolean active() { return generation == StorageEventDispatcher.generation && scope == scope(); }
    }
    private StorageEventDispatcher() { }
}
