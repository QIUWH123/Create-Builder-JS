package net.quiwh.cbjs.kubejs;

import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

public final class StorageFormedEventJS extends StorageStructureEventJS {
    public StorageFormedEventJS(StorageSnapshot previous, StorageDimensions current) { super(previous, current); }
}
