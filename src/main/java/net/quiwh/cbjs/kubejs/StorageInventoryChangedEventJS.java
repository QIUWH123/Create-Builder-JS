package net.quiwh.cbjs.kubejs;

import java.util.List;
import net.quiwh.cbjs.storage.StorageSnapshot;

/** Copies only mutated slots, with the first and completed contents for each member/slot. */
public final class StorageInventoryChangedEventJS extends StorageEventJS {
    private final List<StorageSlotChange> changes;
    public StorageInventoryChangedEventJS(StorageSnapshot controller, List<StorageSlotChange> changes) {
        super(controller, controller.dimensions());
        this.changes = List.copyOf(changes);
    }
    public List<StorageSlotChange> getChanges() { return changes; }
    public int getChangedSlotCount() { return changes.size(); }
}
