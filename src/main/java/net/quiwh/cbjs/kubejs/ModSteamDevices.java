package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.SteamClientModels;

public final class ModSteamDevices {
    private static final java.util.List<SteamEngineBuilder> ENGINES = new java.util.ArrayList<>();
    private static final java.util.List<SteamWhistleBuilder> WHISTLES = new java.util.ArrayList<>();
    private static boolean registered;
    static void add(SteamEngineBuilder builder) { ENGINES.add(builder); }
    static void add(SteamWhistleBuilder builder) { WHISTLES.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        net.quiwh.cbjs.CBJS.LOGGER.info("CBJS steam registration: {} engines, {} whistles", ENGINES.size(), WHISTLES.size());
        for (var engine : ENGINES)
            BlockStressValues.CAPACITIES.register(engine.get(), () -> BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE.get()));
        ClientRegistration.models(() -> () -> {
            ENGINES.forEach(SteamClientModels::declare); WHISTLES.forEach(SteamClientModels::declare);
        });
        registered = true;
    }
    private ModSteamDevices() { }
}
