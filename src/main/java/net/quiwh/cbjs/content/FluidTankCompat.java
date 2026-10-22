package net.quiwh.cbjs.content;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Only scripted tanks replace Create's native identity and attachment decisions. */
public final class FluidTankCompat {
    public static boolean steamAllowed(BlockState state, boolean nativeResult) {
        return state.getBlock() instanceof ScriptedFluidTankBlock tank
                ? tank.sourceBuilder().steamEngineAllowed() : nativeResult;
    }

    public static FluidTankBlockEntity steamSource(FluidTankBlockEntity tank) {
        return tank == null || !steamAllowed(tank.getBlockState(), true) ? null : tank;
    }

    public static boolean bulkTank(Block itemBlock, BlockState state) {
        if (itemBlock instanceof ScriptedFluidTankBlock || state.getBlock() instanceof ScriptedFluidTankBlock)
            return state.getBlock() == itemBlock;
        return FluidTankBlock.isTank(state);
    }

    public static BlockEntityType<?> bulkType(Block itemBlock, BlockEntityType<?> nativeType) {
        return itemBlock instanceof ScriptedFluidTankBlock tank ? tank.getBlockEntityType() : nativeType;
    }

    public static int itemCapacity(Block itemBlock, int nativeCapacity) {
        return itemBlock instanceof ScriptedFluidTankBlock tank
                ? tank.sourceBuilder().capacityPerBlock() : nativeCapacity;
    }

    private FluidTankCompat() { }
}
