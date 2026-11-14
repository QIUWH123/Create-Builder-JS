package net.quiwh.cbjs.kubejs;

import java.util.ArrayList;
import java.util.List;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.client.ArmGaugeClientModels;

public final class ModArmGaugeMachines {
    private static final List<NativeKineticMachineBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(NativeKineticMachineBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        net.quiwh.cbjs.CBJS.LOGGER.info("CBJS arm/gauge registration: {} builders", BUILDERS.size());
        for (var builder : BUILDERS) BlockStressValues.IMPACTS.register(builder.get(), () ->
                BlockStressValues.getImpact(BuiltInRegistries.BLOCK.get(builder.nativeBlockId())));
        ClientRegistration.models(() -> () -> BUILDERS.forEach(ArmGaugeClientModels::declare));
        registered = true;
    }
    /** Common setup runs after Create's display-source registry has completed. */
    public static void registerDisplaySources() {
        for (var builder : BUILDERS) if (builder instanceof GaugeBuilder gauge)
            com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK.add(builder.get(),
                    (gauge.stress ? com.simibubi.create.AllDisplaySources.KINETIC_STRESS : com.simibubi.create.AllDisplaySources.KINETIC_SPEED).get());
    }
    private ModArmGaugeMachines() { }
}
