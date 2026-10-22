package net.quiwh.cbjs.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kubejs.ModFluidTanks;

/** Dedicated-server-safe registration of script tank renderers. */
@Mod.EventBusSubscriber(modid = CBJS.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class FluidTankClientRegistration {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModFluidTanks.forEach((builder, type) ->
                event.registerBlockEntityRenderer(type, builder.isVessel()
                        ? FluidTankClientModels.MaterialVesselRenderer::new : FluidTankClientModels.MaterialTankRenderer::new));
    }

    private FluidTankClientRegistration() { }
}
