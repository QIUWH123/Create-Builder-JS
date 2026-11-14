package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;

/** Live native capacity suppliers, generator annotations and native client registration. */
public final class ModNativeGenerators {
    private static final List<NativeKineticMachineBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(NativeKineticMachineBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        net.quiwh.cbjs.CBJS.LOGGER.info("CBJS native generator registration: {} builders", BUILDERS.size());
        for (var builder : BUILDERS) {
            var nativeBlock = BuiltInRegistries.BLOCK.get(builder.nativeBlockId());
            BlockStressValues.CAPACITIES.register(builder.get(), () -> BlockStressValues.getCapacity(nativeBlock));
            int speed = builder instanceof WaterWheelBuilder wheel ? wheel.large ? 4 : 8 : builder instanceof HandCrankBuilder ? 32 : builder instanceof CreativeMotorBuilder ? 256 : 16;
            BlockStressValues.setGeneratorSpeed(speed, builder instanceof WindmillBearingBuilder || builder instanceof CreativeMotorBuilder).accept(builder.get());
            if (builder instanceof HandCrankBuilder) ItemUseOverrides.addBlock(builder.get());
        }
        ClientRegistration.models(() -> () -> BUILDERS.forEach(NativeGeneratorClientModels::declare));
        ClientRegistration.run(() -> () -> BUILDERS.forEach(NativeGeneratorClientModels::register));
        registered = true;
    }
    private ModNativeGenerators() { }
}
