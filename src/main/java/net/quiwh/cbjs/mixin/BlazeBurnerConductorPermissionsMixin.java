package net.quiwh.cbjs.mixin;

import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.trains.entity.CarriageContraption;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.BlazeBurnerFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Revalidate cached train conductors once when saved custom burners are restored. */
@Mixin(value = CarriageContraption.class, remap = false)
public abstract class BlazeBurnerConductorPermissionsMixin {
    @Inject(method = "readNBT", at = @At("RETURN"), require = 1, allow = 1)
    private void cbjs$loadedConductorPermissions(Level level, CompoundTag tag, boolean spawnPacket,
            CallbackInfo callback) {
        var carriage = (CarriageContraption) (Object) this;
        var blocks = carriage.getBlocks();
        if (blocks.values().stream().noneMatch(info -> BlazeBurnerFamilies.builder(info.state()) != null)) return;

        // Create persists only two aggregate booleans, so preserve other registered conductors
        // by repeating its assembly criteria against the complete restored carriage.
        Couple<Boolean> conductors = Couple.create(false, false);
        var assemblyDirection = carriage.getAssemblyDirection();
        for (var entry : blocks.entrySet()) {
            var state = entry.getValue().state();
            var builder = BlazeBurnerFamilies.builder(state);
            if (builder != null && !builder.options().canControlTrain) continue;
            var interaction = MovingInteractionBehaviour.REGISTRY.get(state.getBlock());
            if (!(interaction instanceof ConductorBlockInteractionBehavior conductor)
                    || !conductor.isValidConductor(state)) continue;
            for (var direction : Iterate.directionsInAxis(assemblyDirection.getAxis())) {
                if (carriage.inControl(entry.getKey(), direction))
                    conductors.set(direction != assemblyDirection, true);
            }
        }
        carriage.blockConductors = conductors;
    }
}
