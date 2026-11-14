package net.quiwh.cbjs.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Horizontal tank identity; window selection and persistence are shared with vertical tanks. */
public class ScriptedFluidVesselBlockEntity extends ScriptedFluidTankBlockEntity {
    public ScriptedFluidVesselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
}
