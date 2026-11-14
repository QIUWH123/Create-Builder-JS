package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelMovementBehaviour;
import com.simibubi.create.content.processing.basin.BasinMovementBehaviour;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.LogisticsClientModels;

/** Startup registration hooks for native rendering, movement and display-link integration. */
public final class ModLogistics {
    private static final java.util.List<FunnelBuilder> FUNNELS = new java.util.ArrayList<>();
    private static final java.util.List<TunnelBuilder> TUNNELS = new java.util.ArrayList<>();
    private static final java.util.List<BasinBuilder> BASINS = new java.util.ArrayList<>();
    private static boolean registered;
    static void add(FunnelBuilder builder) { FUNNELS.add(builder); }
    static void add(TunnelBuilder builder) { TUNNELS.add(builder); }
    static void add(BasinBuilder builder) { BASINS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        net.quiwh.cbjs.CBJS.LOGGER.info("CBJS logistics registration: {} funnels, {} tunnels, {} basins", FUNNELS.size(), TUNNELS.size(), BASINS.size());
        ClientRegistration.models(() -> () -> {
            FUNNELS.forEach(LogisticsClientModels::declare); TUNNELS.forEach(LogisticsClientModels::declare);
        });
        ClientRegistration.run(() -> () -> {
            TUNNELS.stream().filter(b -> b.brass).forEach(LogisticsClientModels::registerConnectedTextures);
        });
        registered = true;
    }
    public static void registerMovement() {
        for (var builder : TUNNELS) {
            com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK.add(builder.get(), com.simibubi.create.AllDisplaySources.ACCUMULATE_ITEMS.get());
            com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK.add(builder.get(), com.simibubi.create.AllDisplaySources.ITEM_THROUGHPUT.get());
        }
        for (var builder : FUNNELS) MovementBehaviour.REGISTRY.register(builder.get(), builder.brass ? FunnelMovementBehaviour.brass() : FunnelMovementBehaviour.andesite());
        for (var builder : BASINS) {
            MovementBehaviour.REGISTRY.register(builder.get(), new BasinMovementBehaviour());
            com.simibubi.create.api.packager.unpacking.UnpackingHandler.REGISTRY.register(builder.get(), com.simibubi.create.impl.unpacking.BasinUnpackingHandler.INSTANCE);
        }
    }
    private ModLogistics() { }
}
