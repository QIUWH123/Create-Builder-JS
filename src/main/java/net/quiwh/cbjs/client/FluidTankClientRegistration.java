package net.quiwh.cbjs.client;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.quiwh.cbjs.kubejs.ModFluidTanks;

/** Dedicated-server-safe registration of script tank renderers. */
public final class FluidTankClientRegistration {
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModFluidTanks.forEach((builder, type) ->
                event.registerBlockEntityRenderer(type, builder.isVessel()
                        ? FluidTankClientModels.MaterialVesselRenderer::new : FluidTankClientModels.MaterialTankRenderer::new));
    }

    private FluidTankClientRegistration() { }
}
