package net.quiwh.cbjs.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.SyncedMountedStorage;
import com.simibubi.create.api.contraption.storage.fluid.WrapperMountedFluidStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateCodecs;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/** Native tank storage lifecycle with the script material retained by its codec. */
public final class ScriptedFluidTankMountedStorage
        extends WrapperMountedFluidStorage<ScriptedFluidTankMountedStorage.Handler> implements SyncedMountedStorage {
    public static final Codec<ScriptedFluidTankMountedStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("tank").forGetter(storage -> storage.blockId),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("capacity").forGetter(storage -> storage.wrapped.getCapacity()),
            CreateCodecs.FLUID_STACK_CODEC.fieldOf("fluid").forGetter(storage -> storage.wrapped.getFluid()),
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("blocks", 0).forGetter(storage -> storage.blocks)
    ).apply(instance, ScriptedFluidTankMountedStorage::new));

    private final ResourceLocation blockId;
    /** Zero identifies legacy saves whose capacity cannot be safely mapped to geometry. */
    private final int blocks;
    private boolean dirty;

    private ScriptedFluidTankMountedStorage(ResourceLocation blockId, int capacity, FluidStack stack, int blocks) {
        super(ModFluidTankMountedStorage.TYPE.get(), new Handler(capacity, BuiltInRegistries.BLOCK.get(blockId), stack, blocks));
        this.blockId = blockId;
        this.blocks = blocks;
        wrapped.onChange = () -> dirty = true;
    }

    public static ScriptedFluidTankMountedStorage fromTank(FluidTankBlockEntity tank) {
        FluidTank inventory = tank.getTankInventory();
        return new ScriptedFluidTankMountedStorage(BuiltInRegistries.BLOCK.getKey(tank.getBlockState().getBlock()),
                inventory.getCapacity(), inventory.getFluid().copy(),
                Math.multiplyExact(Math.multiplyExact(tank.getWidth(), tank.getWidth()), tank.getHeight()));
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity entity) {
        if (entity instanceof ScriptedFluidTankBlockEntity tank && tank.isController()
                && blockId.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) {
            FluidTank inventory = tank.getTankInventory();
            // A lower script capacity must not discard fluid already held by the contraption.
            inventory.setFluid(wrapped.getFluid().copy());
        }
    }

    @Override public boolean isDirty() {
        // Reuse Create's existing synchronization query; do not add a contraption scan.
        wrapped.getCapacity();
        return dirty;
    }
    @Override public void markClean() { dirty = false; }

    @Override
    public void afterSync(Contraption contraption, BlockPos localPos) {
        BlockEntity entity = contraption.getBlockEntityClientSide(localPos);
        if (!(entity instanceof ScriptedFluidTankBlockEntity tank)) return;
        FluidTank inventory = tank.getTankInventory();
        inventory.setCapacity(wrapped.getCapacity());
        inventory.setFluid(wrapped.getFluid().copy());
        float fillLevel = inventory.getFluidAmount() / (float)inventory.getCapacity();
        if (tank.getFluidLevel() == null)
            tank.setFluidLevel(LerpedFloat.linear().startWithValue(fillLevel));
        tank.getFluidLevel().chase(fillLevel, 0.5, LerpedFloat.Chaser.EXP);
    }

    public static final class Handler extends FluidTank {
        private Runnable onChange = () -> { };
        private final Block block;
        private final int blocks;

        public Handler(int capacity, Block block, FluidStack stack) {
            this(capacity, block, stack, 0);
        }

        private Handler(int capacity, Block block, FluidStack stack, int blocks) {
            super(currentCapacity(capacity, block, blocks),
                    fluid -> block instanceof ScriptedFluidTankBlock tank && tank.sourceBuilder().acceptsFluid(fluid));
            this.block = block;
            this.blocks = blocks;
            // Existing contents survive filter and capacity changes; only new intake is limited.
            setFluid(stack.copy());
        }

        private static int currentCapacity(int saved, Block block, int blocks) {
            if (blocks == 0 || !(block instanceof ScriptedFluidTankBlock tank)) return saved;
            return (int) Math.min(Integer.MAX_VALUE, (long) tank.sourceBuilder().capacityPerBlock() * blocks);
        }

        /** Refresh only when accessed; inactive contraptions need no scan or periodic work. */
        private void refreshCapacity() {
            int current = currentCapacity(super.getCapacity(), block, blocks);
            if (current != super.getCapacity()) {
                super.setCapacity(current);
                onChange.run();
            }
        }

        @Override public int getCapacity() { refreshCapacity(); return super.getCapacity(); }
        @Override public int getSpace() { refreshCapacity(); return super.getSpace(); }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            // Forge's fill can otherwise return a negative amount and truncate saved overflow.
            if (getFluidAmount() >= getCapacity()) return 0;
            return super.fill(resource, action);
        }

        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            refreshCapacity();
            return super.drain(resource, action);
        }

        @Override public FluidStack drain(int amount, FluidAction action) {
            refreshCapacity();
            return super.drain(amount, action);
        }

        @Override protected void onContentsChanged() { onChange.run(); }
    }
}
