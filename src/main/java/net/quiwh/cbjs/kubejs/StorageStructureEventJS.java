package net.quiwh.cbjs.kubejs;

import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

/** Old dimensions belong to the reported controller; zero new dimensions mean it was removed. */
public abstract class StorageStructureEventJS extends StorageEventJS {
    private final StorageDimensions previous;
    protected StorageStructureEventJS(StorageSnapshot previous, StorageDimensions current) {
        super(previous, current);
        this.previous = previous.dimensions();
    }
    public String getOldAxis() { return previous.axis().getName(); }
    public int getOldWidth() { return previous.width(); }
    public int getOldLength() { return previous.length(); }
    public int getOldHeight() { return previous.sizeY(); }
    public int getOldSizeX() { return previous.sizeX(); }
    public int getOldSizeY() { return previous.sizeY(); }
    public int getOldSizeZ() { return previous.sizeZ(); }
    public int getOldBlockCount() { return previous.blockCount(); }
    public String getNewAxis() { return getAxis(); }
    public int getNewWidth() { return getWidth(); }
    public int getNewLength() { return getLength(); }
    public int getNewHeight() { return getHeight(); }
    public int getNewSizeX() { return getSizeX(); }
    public int getNewSizeY() { return getSizeY(); }
    public int getNewSizeZ() { return getSizeZ(); }
    public int getNewBlockCount() { return getBlockCount(); }
}
