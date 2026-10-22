package net.quiwh.cbjs.content;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Integer storage bounds and finite connectivity searches without an arbitrary size policy. */
@dev.latvian.mods.rhino.util.HideFromJS
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "cbjs")
public final class StorageStructureLimits {
    private static volatile int configGeneration;
    private static volatile boolean storagePresent;
    private static int lastCapacity = -1;
    private static int lastHeight = -1;
    private static int lastSlots = -1;

    public static int configGeneration() {
        if (!storagePresent) storagePresent = true;
        return configGeneration;
    }

    /** Forge dispatches config events only to the owning mod. Poll three cached values once
     * per server tick instead, then let loaded blocks apply changes on the server thread. */
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (!storagePresent || event.phase != net.minecraftforge.event.TickEvent.Phase.START) return;
        var config = com.simibubi.create.infrastructure.config.AllConfigs.server();
        if (config.specification == null || !config.specification.isLoaded()) return;
        int capacity = config.fluids.fluidTankCapacity.get();
        int height = config.fluids.fluidTankMaxHeight.get();
        int slots = config.logistics.vaultCapacity.get();
        if (capacity == lastCapacity && height == lastHeight && slots == lastSlots) return;
        lastCapacity = capacity;
        lastHeight = height;
        lastSlots = slots;
        configGeneration++;
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onServerStopped(net.minecraftforge.event.server.ServerStoppedEvent event) {
        storagePresent = false;
        lastCapacity = lastHeight = lastSlots = -1;
        configGeneration++;
    }

    public static int boundedCapacity(int capacity, int width, int length) {
        if (width < 1 || length < 1) throw new IllegalArgumentException("Storage dimensions must be positive");
        if (Integer.MAX_VALUE / (long) capacity / width / width < length) return Integer.MAX_VALUE;
        return capacity * width * width * length;
    }
    public static int maximumWidth(int capacity) {
        return (int) Math.sqrt(Integer.MAX_VALUE / (long) capacity);
    }

    public static int maximumLength(int capacity, int width) {
        if (width < 1) throw new IllegalArgumentException("Storage width must be positive");
        return (int) (Integer.MAX_VALUE / (long) capacity / width / width);
    }

    /** Native connectivity tests every width; only existing loaded rows can form a square. */
    public static int loadedWidth(BlockEntity entity, IMultiBlockEntityContainer storage, int maximum) {
        var level = entity.getLevel();
        if (level == null || level.isClientSide) return maximum;
        int result = maximum;
        Direction.Axis main = storage.getMainConnectionAxis();
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (axis == main) continue;
            int span = 1;
            for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
                Direction direction = Direction.fromAxisAndDirection(axis, sign);
                for (int distance = 1; distance < maximum && span < maximum; distance++) {
                    BlockPos pos = entity.getBlockPos().relative(direction, distance);
                    if (!level.hasChunkAt(pos)) break;
                    BlockEntity other = level.getBlockEntity(pos);
                    if (other == null || other.getType() != entity.getType()
                            || !other.getBlockState().is(entity.getBlockState().getBlock())
                            || !(other instanceof IMultiBlockEntityContainer part)
                            || part.getMainConnectionAxis() != main) break;
                    span++;
                }
            }
            result = Math.min(result, span);
        }
        // Missing chunks must not turn a previously valid structure into a configured shrink.
        return Math.min(maximum, Math.max(storage.getWidth(), result));
    }

    private StorageStructureLimits() { }
}
