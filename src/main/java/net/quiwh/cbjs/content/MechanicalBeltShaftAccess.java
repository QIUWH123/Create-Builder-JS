package net.quiwh.cbjs.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** Per-segment decode cache; avoids parsing saved shaft state every rendered frame. */
public interface MechanicalBeltShaftAccess {
    BlockState cbjs$shaftMaterial(CompoundTag data);
}
