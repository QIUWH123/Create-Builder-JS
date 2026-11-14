package net.quiwh.cbjs.kubejs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.GearboxClientModels;

/** Registers moving partials after scripted blocks exist, before client model baking. */
public final class ModGearboxes {
    private static final List<GearboxBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(GearboxBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        ClientRegistration.models(() -> () -> BUILDERS.forEach(GearboxClientModels::declareModels));
        registered = true;
    }
    private ModGearboxes() { }
}
