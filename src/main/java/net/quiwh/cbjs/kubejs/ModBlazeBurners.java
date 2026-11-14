package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.boiler.BoilerHeater;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.interaction.ConductorBlockInteractionBehavior;
import com.simibubi.create.content.processing.burner.BlazeBurnerMovementBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.BlazeBurnerClientModels;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlockEntities;

/** Native registries use per-block identities, so every scripted burner is associated explicitly. */
public final class ModBlazeBurners {
    private static final List<BlazeBurnerBuilder> BUILDERS = new ArrayList<>();
    static void add(BlazeBurnerBuilder builder) { BUILDERS.add(builder); ScriptedBlazeBurnerBlockEntities.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        net.quiwh.cbjs.CBJS.LOGGER.info("CBJS burner registration: {} builders", BUILDERS.size());
        ClientRegistration.models(() -> () -> BUILDERS.forEach(BlazeBurnerClientModels::declare));
        ClientRegistration.run(() -> () -> BUILDERS.forEach(BlazeBurnerClientModels::register));
    }
    public static void registerMovement() {
        for (var builder : BUILDERS) {
            BoilerHeater.REGISTRY.register(builder.get(), (level, pos, state) -> builder.options().canHeatBoiler
                    ? BoilerHeater.BLAZE_BURNER.getHeat(level, pos, state.setValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL,
                            builder.options().clamp(state.getValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL))))
                    : BoilerHeater.NO_HEAT);
            BoilerHeater.REGISTRY.register(builder.litBlock(), (level, pos, state) -> builder.options().canHeatBoiler ? BoilerHeater.PASSIVE_HEAT : BoilerHeater.NO_HEAT);
            MovementBehaviour.REGISTRY.register(builder.get(), new BlazeBurnerMovementBehaviour());
            if (builder.options().canControlTrain) MovingInteractionBehaviour.REGISTRY.register(builder.get(), new ConductorBlockInteractionBehavior.BlazeBurner());
        }
    }
    private ModBlazeBurners() { }
}
