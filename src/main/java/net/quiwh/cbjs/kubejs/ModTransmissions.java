package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.TransmissionClientModels;

public final class ModTransmissions {
    private static final List<TransmissionBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(TransmissionBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (TransmissionBuilder builder : BUILDERS) BlockStressValues.IMPACTS.register(builder.get(), () -> 0);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BUILDERS.forEach(TransmissionClientModels::register));
        registered = true;
    }
    private ModTransmissions() { }
}
