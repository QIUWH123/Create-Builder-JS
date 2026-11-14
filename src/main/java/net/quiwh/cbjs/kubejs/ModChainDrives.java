package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.ChainDriveClientModels;

import java.util.ArrayList;
import java.util.List;

/** Register moving models after blocks exist and before Minecraft loads resources. */
public final class ModChainDrives {
    private static final List<ChainDriveBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;

    static void add(ChainDriveBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (ChainDriveBuilder builder : BUILDERS) BlockStressValues.IMPACTS.register(builder.get(), () -> 0);
        ClientRegistration.models(() -> () -> BUILDERS.forEach(ChainDriveClientModels::declareModels));
        registered = true;
    }
    private ModChainDrives() { }
}
