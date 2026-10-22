package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.TorquePropagator;
import java.util.Map;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = TorquePropagator.class, remap = false)
public interface TorqueNetworksAccessor {
    @Accessor(value = "networks", remap = false)
    static Map<LevelAccessor, Map<Long, KineticNetwork>> cbjs$getNetworks() {
        throw new AssertionError("Torque network accessor was not transformed");
    }
}
