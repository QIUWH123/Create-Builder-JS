package net.quiwh.cbjs.kubejs;

import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

public final class StorageResizedEventJS extends StorageStructureEventJS {
    public StorageResizedEventJS(StorageSnapshot previous, StorageDimensions current) { super(previous, current); }
}
