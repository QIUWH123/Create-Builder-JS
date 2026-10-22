package net.quiwh.cbjs.content;

import com.google.common.collect.ImmutableMap;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageWrapper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
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
            offsets.slots();
            return super.getIndexForSlot(slot);
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
            offsets.slots();
            return super.getIndexForSlot(slot);
        }
    }
    private static final class Offsets {
        private final IItemHandlerModifiable[] handlers;
        private final int[] baseIndex;
        private int slotCount;
        private int configuredCapacity;
        private long revision;
        private Offsets(IItemHandlerModifiable[] handlers, int[] baseIndex, int slotCount) {
            this.handlers = handlers;
            this.baseIndex = baseIndex;
            this.slotCount = slotCount;
            configuredCapacity = AllConfigs.server().logistics.vaultCapacity.get();
            revision = CAPACITY_REVISION.get();
        }
        private int slots() {
            int current = AllConfigs.server().logistics.vaultCapacity.get();
            if (current != configuredCapacity || revision != CAPACITY_REVISION.get()) {
                int total = 0;
                // Native CombinedInvWrapper freezes these cumulative offsets in its constructor.
                // Refresh children first, including the mounted wrapper inside allItems, so
                // cached capabilities and external inventories retain the right slot routing.
                for (int index = 0; index < handlers.length; index++) {
                    total = Math.addExact(total, handlers[index].getSlots());
                    baseIndex[index] = total;
                }
                slotCount = total;
                configuredCapacity = current;
                revision = CAPACITY_REVISION.get();
            }
            return slotCount;
        }
    }
    private MountedItemStorageCapacity() { }
}
