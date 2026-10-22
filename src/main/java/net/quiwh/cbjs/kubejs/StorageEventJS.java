package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.storage.StorageDimensions;
import net.quiwh.cbjs.storage.StorageSnapshot;

/** All storage events identify one controller and capture its completed world dimensions. */
public abstract class StorageEventJS extends LevelEventJS {
    private final StorageSnapshot snapshot;
    private final StorageDimensions dimensions;
    private final String blockId;

    protected StorageEventJS(StorageSnapshot snapshot, StorageDimensions dimensions) {
        this.snapshot = snapshot;
        this.dimensions = dimensions;
        blockId = ForgeRegistries.BLOCKS.getKey(snapshot.state().getBlock()).toString();
    }
    public String getBlockId() { return blockId; }
    public BlockContainerJS getBlock() { return new BlockContainerJS(getLevel(), getPos()); }
    public BlockContainerJS getController() { return getBlock(); }
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
