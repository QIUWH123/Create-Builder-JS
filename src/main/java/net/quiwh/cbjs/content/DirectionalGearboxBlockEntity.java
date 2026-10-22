package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.quiwh.cbjs.kinetics.GearboxTransmission;

/** Create Builder JS owns the transmission; base Create supplies normal network and persistence behavior. */
public final class DirectionalGearboxBlockEntity extends SplitShaftBlockEntity {
    public DirectionalGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public float getRotationSpeedModifier(Direction face) {
        if (!(getBlockState().getBlock() instanceof ScriptedDirectionalGearboxBlock block)) return 1;
        Direction input = source == null ? null : Direction.getNearest(source.getX() - worldPosition.getX(),
                source.getY() - worldPosition.getY(), source.getZ() - worldPosition.getZ());
        return GearboxTransmission.modifier(block.sourceBuilder().kind,
                getBlockState().getValue(BlockStateProperties.AXIS), input, face);
    }

    /** Both render paths consume the exact same per-port speed as the kinetic network. */
    public float getOutputSpeed(Direction face) { return getSpeed() * getRotationSpeedModifier(face); }

    @Override protected boolean isNoisy() { return false; }
}
