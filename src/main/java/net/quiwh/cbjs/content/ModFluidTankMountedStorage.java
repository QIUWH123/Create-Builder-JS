package net.quiwh.cbjs.content;

import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.CBJS;
import org.jetbrains.annotations.Nullable;

/** A separate codec keeps the material and its filter when a contraption is saved. */
public final class ModFluidTankMountedStorage {
    public static final RegistryEntry<Type> TYPE = CBJS.REGISTRATE
            .mountedFluidStorage("scripted_fluid_tank", Type::new).register();

    public static void register() { }

    public static void associate(Block block) {
        if (block instanceof ScriptedFluidTankBlock)
            MountedFluidStorageType.REGISTRY.register(block, TYPE.get());
    }

    public static final class Type extends MountedFluidStorageType<ScriptedFluidTankMountedStorage> {
        public Type() { super(ScriptedFluidTankMountedStorage.CODEC); }

        @Override
        public @Nullable ScriptedFluidTankMountedStorage mount(Level level, BlockState state, BlockPos pos,
                @Nullable BlockEntity entity) {
            if (state.getBlock() instanceof ScriptedFluidTankBlock && entity instanceof ScriptedFluidTankBlockEntity tank
                    && tank.isController())
                return ScriptedFluidTankMountedStorage.fromTank(tank);
            return null;
        }
    }

    private ModFluidTankMountedStorage() { }
}
