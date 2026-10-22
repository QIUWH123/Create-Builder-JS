package net.quiwh.cbjs.kubejs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.GearboxClientModels;

/** Registers moving partials after scripted blocks exist, before client model baking. */
public final class ModGearboxes {
    private static final List<GearboxBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(GearboxBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> BUILDERS.forEach(GearboxClientModels::register));
        registered = true;
    }
    private ModGearboxes() { }
}
