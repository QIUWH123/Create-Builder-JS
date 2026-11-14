package net.quiwh.cbjs.content;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import java.util.concurrent.atomic.AtomicLong;

/** Keep native mounted wrapper references usable when scripted local inventories gain slots. */
@dev.latvian.mods.rhino.util.HideFromJS
public final class MountedItemStorageCapacity {
    private static final AtomicLong CAPACITY_REVISION = new AtomicLong();

    static void localSlotsChanged() { CAPACITY_REVISION.incrementAndGet(); }
    public static boolean containsScripted(ImmutableMap<BlockPos, MountedItemStorage> storages) {
        return storages.values().stream().anyMatch(ModItemVaultMountedStorage.Storage.class::isInstance);
    }
    public static boolean containsDynamic(IItemHandlerModifiable[] handlers) {
        for (var handler : handlers) if (handler instanceof Mounted || handler instanceof Combined) return true;
        return false;
    }

    public static final class Mounted extends MountedItemStorageWrapper {
        private final Offsets offsets;
        public Mounted(ImmutableMap<BlockPos, MountedItemStorage> storages) {
            super(storages);
            offsets = new Offsets(itemHandler, baseIndex, slotCount);
        }
        @Override public int getSlots() { return offsets == null ? super.getSlots() : offsets.slots(); }
        @Override protected int getIndexForSlot(int slot) {
            return offsets == null ? super.getIndexForSlot(slot) : offsets.indexForSlot(slot);
        }
        @Override protected int getSlotFromIndex(int slot, int index) {
            return offsets == null ? super.getSlotFromIndex(slot, index) : offsets.slotFromIndex(slot, index);
        }
    }
    public static final class Combined extends CombinedInvWrapper {
        private final Offsets offsets;
        public Combined(IItemHandlerModifiable[] handlers) {
            super(handlers);
            offsets = new Offsets(itemHandler, baseIndex, slotCount);
        }
        @Override public int getSlots() { return offsets == null ? super.getSlots() : offsets.slots(); }
        @Override protected int getIndexForSlot(int slot) {
            return offsets == null ? super.getIndexForSlot(slot) : offsets.indexForSlot(slot);
        }
        @Override protected int getSlotFromIndex(int slot, int index) {
            return offsets == null ? super.getSlotFromIndex(slot, index) : offsets.slotFromIndex(slot, index);
        }
    }
    private static final class Offsets {
        private final IItemHandlerModifiable[] handlers;
        private final int[] baseIndex;
        private final int[] endOffsets;
        private int slotCount;
        private int configuredCapacity;
        private long revision;
        private Offsets(IItemHandlerModifiable[] handlers, int[] baseIndex, int slotCount) {
            this.handlers = handlers;
            this.baseIndex = baseIndex;
            this.endOffsets = new int[handlers.length];
            this.slotCount = slotCount;
            System.arraycopy(baseIndex, 0, endOffsets, 0, Math.min(baseIndex.length, endOffsets.length));
            configuredCapacity = AllConfigs.server().logistics.vaultCapacity.get();
            revision = CAPACITY_REVISION.get();
        }
        private int slots() {
            int current = AllConfigs.server().logistics.vaultCapacity.get();
            if (current != configuredCapacity || revision != CAPACITY_REVISION.get()) {
                refresh();
                configuredCapacity = current;
                revision = CAPACITY_REVISION.get();
            }
            return slotCount;
        }
        private void refresh() {
            int total = 0;
            // Native wrappers freeze these cumulative offsets in their constructors. Refresh
            // child handlers first, including nested dynamic wrappers, before routing requests.
            for (int index = 0; index < handlers.length; index++) {
                int childSlots = handlers[index].getSlots();
                if (childSlots < 0) throw new IllegalStateException("Negative item-handler slot count");
                total = Math.addExact(total, childSlots);
                endOffsets[index] = total;
                if (index < baseIndex.length) baseIndex[index] = total;
            }
            slotCount = total;
        }
        private int indexForSlot(int slot) {
            int total = slots();
            if (slot < 0 || slot >= total) return -1;
            for (int index = 0; index < endOffsets.length; index++)
                if (slot < endOffsets[index]) return index;
            return -1;
        }
        private int slotFromIndex(int slot, int index) {
            slots();
            if (index <= 0 || index >= endOffsets.length) return slot;
            return slot - endOffsets[index - 1];
        }
    }
    private MountedItemStorageCapacity() { }
}
