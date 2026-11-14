package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.quiwh.cbjs.mixin.GogglesKineticDataAccess;

/** Uses only fields already synchronized by Create; never walks or creates a kinetic network. */
public final class GogglesKineticInfo {
    private final float rpm, theoreticalRpm;
    private final Float stress, capacity;
    private final boolean overstressed;
    private GogglesKineticInfo(KineticBlockEntity entity) {
        rpm = entity.getSpeed(); theoreticalRpm = entity.getTheoreticalSpeed(); overstressed = entity.isOverStressed();
        var access = (Object) entity instanceof GogglesKineticDataAccess values ? values : null;
        stress = access == null ? null : access.cbjs$stress();
        capacity = access == null ? null : access.cbjs$capacity();
    }
    @dev.latvian.mods.rhino.util.HideFromJS
    public static GogglesKineticInfo of(BlockEntity entity) { return entity instanceof KineticBlockEntity kinetic ? new GogglesKineticInfo(kinetic) : null; }
    public float getRpm() { return rpm; }
    public float getTheoreticalRpm() { return theoreticalRpm; }
    public Float getStress() { return stress; }
    public Float getCapacity() { return capacity; }
    public boolean getIsOverstressed() { return overstressed; }
}
