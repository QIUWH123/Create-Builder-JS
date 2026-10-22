package net.quiwh.cbjs.content;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.logistics.vault.ItemVaultBlock;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.VersionedInventoryWrapper;
import com.simibubi.create.foundation.utility.SameSizeCombinedInvWrapper;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.quiwh.cbjs.kubejs.ItemVaultBuilder;
import net.quiwh.cbjs.storage.StorageEventDispatcher;

/** Native per-block inventories and connectivity lifecycle with material and XYZ geometry supplied locally. */
public class ScriptedItemVaultBlockEntity extends ItemVaultBlockEntity {
    private int migrationRetry;
    private boolean readingStorage;
    private int configGeneration = StorageStructureLimits.configGeneration();
    private int configuredCapacity;
    private int configuredMaxWidth;
    private int configuredMaxLength;

    public ScriptedItemVaultBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        configuredCapacity = builder().capacityPerBlock();
        configuredMaxWidth = builder().maximumWidth();
        configuredMaxLength = builder().maximumLength(radius);
        inventory = new Inventory(configuredCapacity);
    }
    public ItemVaultBuilder builder() { return ((ScriptedItemVaultBlock)getBlockState().getBlock()).sourceBuilder(); }
    @Override public Direction.Axis getMainConnectionAxis() {
        return builder().vertical() ? Direction.Axis.Y : getBlockState().getValue(ItemVaultBlock.HORIZONTAL_AXIS);
    }
    @Override public int getMaxWidth() { return StorageStructureLimits.loadedWidth(this, this, builder().maximumWidth()); }
    @Override public int getMaxLength(Direction.Axis axis, int width) { return builder().maximumLength(width); }
    public BlockPos partPosition(int length, int first, int second) {
        return switch (getMainConnectionAxis()) {
            case X -> worldPosition.offset(length, first, second);
            case Y -> worldPosition.offset(first, length, second);
            case Z -> worldPosition.offset(first, second, length);
        };
    }
    @Override public void tick() {
        if (level != null && !level.isClientSide && configGeneration != StorageStructureLimits.configGeneration())
            refreshStorageSettings();
        super.tick();
    }

    /** Resize only this block's local slots, retaining every occupied overflow slot. */
    protected void refreshStorageSettings() {
        configGeneration = StorageStructureLimits.configGeneration();
        int capacity = builder().capacityPerBlock();
        int maxWidth = builder().maximumWidth();
        int maxLength = builder().maximumLength(radius);
        boolean dimensionsChanged = configuredMaxWidth != maxWidth || configuredMaxLength != maxLength;
        boolean capacityChanged = configuredCapacity != capacity;
        configuredCapacity = capacity;
        configuredMaxWidth = maxWidth;
        configuredMaxLength = maxLength;
        if (capacityChanged && ((Inventory) inventory).resizeToCapacity()) {
            invalidateControllerCapability();
            setChanged();
            updateComparators();
            sendData();
        }
        if (dimensionsChanged) {
            updateConnectivity = true;
            migrationRetry = 0;
        }
    }

    public void scheduleConnectivity() { updateConnectivity = true; }
    @Override protected void updateConnectivity() {
        if (!level.isClientSide && isController() && (radius > builder().maximumWidth() || length > getMaxLength(getMainConnectionAxis(), radius))) {
            if (migrationRetry-- > 0) return;
            migrationRetry = 20;
            for (int l = 0; l < length; l++) for (int a = 0; a < radius; a++) for (int b = 0; b < radius; b++) {
                BlockPos pos = partPosition(l, a, b);
                if (!level.hasChunkAt(pos)) return;
                if (level.getBlockEntity(pos) == null && level.getBlockState(pos).is(getBlockState().getBlock())) return;
            }
            ConnectivityHandler.splitMulti(this);
        }
        super.updateConnectivity();
    }
    @Override public ScriptedItemVaultBlockEntity getControllerBE() {
        if (isController()) return this;
        if (level == null || !level.hasChunkAt(controller)) return null;
        var other = level.getBlockEntity(controller);
        return other instanceof ScriptedItemVaultBlockEntity vault && vault.getType() == getType() ? vault : null;
    }
    @Override public void removeController(boolean keepContents) {
        if (level == null || level.isClientSide) return;
        if (!readingStorage) StorageEventDispatcher.structureChanging(this);
        updateConnectivity = true; controller = null; radius = 1; length = 1;
        level.setBlock(worldPosition, getBlockState().setValue(ItemVaultBlock.LARGE, false), 22);
        itemCapability.invalidate(); setChanged(); sendData();
    }
    @Override public void setWidth(int width) {
        if (!readingStorage && width != radius) StorageEventDispatcher.structureChanging(this);
        super.setWidth(width);
        configuredMaxLength = builder().maximumLength(width);
    }
    @Override public void setHeight(int height) {
        if (!readingStorage && height != length) StorageEventDispatcher.structureChanging(this);
        super.setHeight(height);
    }
    @Override public void setController(BlockPos controller) {
        if (!readingStorage && !getController().equals(controller)) StorageEventDispatcher.structureChanging(this);
        super.setController(controller);
    }
    @Override public void remove() {
        StorageEventDispatcher.removed(this);
        super.remove();
    }
    @Override public void notifyMultiUpdated() {
        level.setBlock(worldPosition, getBlockState().setValue(ItemVaultBlock.LARGE, radius > 2), 6);
        itemCapability.invalidate();
        setChanged(); sendData(); updateComparators();
    }
    @Override protected void updateComparators() {
        var root = getControllerBE();
        if (root == null || level == null || level.isClientSide) return;
        // Slot contents remain uncached: third-party handlers may expose mutable ItemStacks.
        Direction.Axis axis = root.getMainConnectionAxis();
        StorageComparatorUpdates.notifySurface(level, root.worldPosition,
                axis == Direction.Axis.X ? root.length : root.radius,
                axis == Direction.Axis.Y ? root.length : root.radius,
                axis == Direction.Axis.Z ? root.length : root.radius);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (isItemHandlerCap(cap)) {
            var root = getControllerBE();
            if (root == null) return LazyOptional.empty();
            root.prepareCapability();
            return root.itemCapability.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override public InventoryIdentifier getInvId() {
        var root = getControllerBE();
        if (root == null) return null;
        root.prepareCapability(); return root.invId;
    }
    private void prepareCapability() {
        if (itemCapability.isPresent()) return;
        List<IItemHandlerModifiable> parts = new ArrayList<>();
        long remainingSlots = Integer.MAX_VALUE;
        for (int l = 0; l < length; l++) for (int a = 0; a < radius; a++) for (int b = 0; b < radius; b++) {
            BlockPos pos = partPosition(l, a, b);
            ScriptedItemVaultBlockEntity part = level != null && level.hasChunkAt(pos)
                    ? ConnectivityHandler.partAt(getType(), level, pos) : null;
            IItemHandlerModifiable local = part != null && part.getController().equals(getController()) ? part.inventory : null;
            if (remainingSlots == 0) local = null;
            if (local != null && local.getSlots() > remainingSlots)
                local = new net.minecraftforge.items.wrapper.RangedWrapper(local, 0, (int) remainingSlots);
            if (local != null) remainingSlots -= local.getSlots();
            parts.add(local);
        }
        IItemHandlerModifiable[] handlers = parts.stream().map(part -> part == null ? new ItemStackHandler(0) : part)
                .toArray(IItemHandlerModifiable[]::new);
        IItemHandler combined = new VersionedInventoryWrapper(SameSizeCombinedInvWrapper.create(handlers));
        itemCapability = LazyOptional.of(() -> combined);
        BlockPos corner = partPosition(length - 1, radius - 1, radius - 1);
        invId = new InventoryIdentifier.Bounds(BoundingBox.fromCorners(worldPosition, corner));
    }
    @Override protected void read(CompoundTag tag, boolean clientPacket) {
        CompoundTag safe = tag.copy();
        safe.putInt("Size", Math.max(1, safe.getInt("Size")));
        safe.putInt("Length", Math.max(1, safe.getInt("Length")));
        // Local occupied inventories survive a config increase until loaded connectivity
        // safely separates structures that no longer fit a combined integer slot count.
        long available = Integer.MAX_VALUE;
        if (available / safe.getInt("Size") / safe.getInt("Size") < safe.getInt("Length"))
            throw new IllegalArgumentException("Saved item storage dimensions exceed supported inventory size at " + worldPosition);
        readingStorage = true;
        try {
            super.read(safe, clientPacket);
        } finally {
            readingStorage = false;
        }
        itemCapability.invalidate();
        if (!clientPacket && isController() && (radius > builder().maximumWidth() || length > getMaxLength(getMainConnectionAxis(), radius)))
            updateConnectivity = true;
    }
    /** Mounted data retains extra occupied slots after a script reduces capacity. */
    @Override public void applyInventoryToBlock(ItemStackHandler source) {
        inventory.deserializeNBT(source.serializeNBT()); invalidateControllerCapability();
    }
    @Override public void initialize() { super.initialize(); invalidateControllerCapability(); }
    @Override public void onChunkUnloaded() { invalidateControllerCapability(); super.onChunkUnloaded(); }
    @Override public void invalidate() { invalidateControllerCapability(); super.invalidate(); itemCapability.invalidate(); }
    private void invalidateControllerCapability() {
        var root = getControllerBE();
        if (root != null) root.itemCapability.invalidate();
    }

    private final class Inventory extends ItemStackHandler {
        private Inventory(int slots) { super(slots); }
        private boolean resizeToCapacity() {
            int target = builder().capacityPerBlock();
            for (int slot = stacks.size() - 1; slot >= target; slot--)
                if (!stacks.get(slot).isEmpty()) { target = slot + 1; break; }
            if (target == stacks.size()) return false;
            var previous = stacks;
            stacks = net.minecraft.core.NonNullList.withSize(target, ItemStack.EMPTY);
            for (int slot = 0; slot < Math.min(target, previous.size()); slot++) stacks.set(slot, previous.get(slot));
            return true;
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot < builder().capacityPerBlock() && builder().acceptsItem(stack);
        }
        @Override protected void onContentsChanged(int slot) {
            if (level != null && !level.isClientSide) { setChanged(); updateComparators(); }
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack previous = previous(slot, simulate);
            ItemStack result = super.insertItem(slot, stack, simulate);
            changed(slot, previous);
            return result;
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack previous = previous(slot, simulate);
            ItemStack result = super.extractItem(slot, amount, simulate);
            changed(slot, previous);
            return result;
        }
        @Override public void setStackInSlot(int slot, ItemStack stack) {
            ItemStack previous = previous(slot, false);
            super.setStackInSlot(slot, stack);
            changed(slot, previous);
        }
        private ItemStack previous(int slot, boolean simulate) {
            return !simulate && !readingStorage && StorageEventDispatcher.needsInventorySnapshot(ScriptedItemVaultBlockEntity.this, slot)
                    ? getStackInSlot(slot).copy() : null;
        }
        private void changed(int slot, ItemStack previous) {
            if (previous != null) StorageEventDispatcher.inventoryChanged(ScriptedItemVaultBlockEntity.this, slot, previous);
        }
        @Override public void deserializeNBT(CompoundTag tag) {
            CompoundTag copy = tag.copy();
            copy.putInt("Size", Math.max(builder().capacityPerBlock(), tag.getInt("Size")));
            super.deserializeNBT(copy);
        }
    }
}
