package net.quiwh.cbjs.storage;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.core.Direction;

/** Native square cross section and longitudinal length, with explicit world dimensions. */
public record StorageDimensions(Direction.Axis axis, int width, int length) {
    public static StorageDimensions of(IMultiBlockEntityContainer storage) {
        return new StorageDimensions(storage.getMainConnectionAxis(), storage.getWidth(), storage.getHeight());
    }
    public int sizeX() { return axis == Direction.Axis.X ? length : width; }
    public int sizeY() { return axis == Direction.Axis.Y ? length : width; }
    public int sizeZ() { return axis == Direction.Axis.Z ? length : width; }
    public int blockCount() { return width * width * length; }
    public StorageDimensions empty() { return new StorageDimensions(axis, 0, 0); }
}
