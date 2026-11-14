package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.TransmissionClientModels;

public final class ModTransmissions {
    private static final List<TransmissionBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(TransmissionBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (TransmissionBuilder builder : BUILDERS) BlockStressValues.IMPACTS.register(builder.get(), () -> 0);
        ClientRegistration.models(() -> () -> BUILDERS.forEach(TransmissionClientModels::declareModels));
        registered = true;
    }
    private ModTransmissions() { }
}
