package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.level.CachedLevelBlock;
import dev.latvian.mods.kubejs.level.KubeLevelEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

/** All storage events identify one controller and capture its completed world dimensions. */
public abstract class StorageEventJS implements KubeLevelEvent {
    private final StorageSnapshot snapshot;
    private final StorageDimensions dimensions;
    private final String blockId;

    protected StorageEventJS(StorageSnapshot snapshot, StorageDimensions dimensions) {
        this.snapshot = snapshot;
        this.dimensions = dimensions;
        blockId = BuiltInRegistries.BLOCK.getKey(snapshot.state().getBlock()).toString();
    }
    public String getBlockId() { return blockId; }
    public CachedLevelBlock getBlock() { return new CachedLevelBlock(getLevel(), getPos()); }
    public CachedLevelBlock getController() { return getBlock(); }
    public BlockPos getPos() { return snapshot.pos(); }
    public BlockPos getControllerPos() { return getPos(); }
    @Override public Level getLevel() { return snapshot.level(); }
    public String getAxis() { return dimensions.axis().getName(); }
    public int getWidth() { return dimensions.width(); }
    public int getLength() { return dimensions.length(); }
    public int getHeight() { return dimensions.sizeY(); }
    public int getSizeX() { return dimensions.sizeX(); }
    public int getSizeY() { return dimensions.sizeY(); }
    public int getSizeZ() { return dimensions.sizeZ(); }
    public int getBlockCount() { return dimensions.blockCount(); }
    net.minecraft.world.level.block.Block filterBlock() { return snapshot.state().getBlock(); }
}
