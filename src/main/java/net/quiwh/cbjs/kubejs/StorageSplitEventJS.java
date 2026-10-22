package net.quiwh.cbjs.kubejs;

import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

public final class StorageSplitEventJS extends StorageStructureEventJS {
    public StorageSplitEventJS(StorageSnapshot previous, StorageDimensions current) { super(previous, current); }
}
