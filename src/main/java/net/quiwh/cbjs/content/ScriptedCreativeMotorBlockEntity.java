package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Native motor's registry-identity guard must accept its scripted subclass. */
public final class ScriptedCreativeMotorBlockEntity extends CreativeMotorBlockEntity {
    public ScriptedCreativeMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override public float getGeneratedSpeed() {
        return convertToDirection(generatedSpeed.getValue(), getBlockState().getValue(CreativeMotorBlock.FACING));
    }
}
