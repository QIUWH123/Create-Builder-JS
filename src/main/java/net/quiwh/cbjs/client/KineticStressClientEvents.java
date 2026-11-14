package net.quiwh.cbjs.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kinetics.KineticStressValues;

/** Connection-scoped stress values must not survive a server switch. */
@EventBusSubscriber(modid = CBJS.MOD_ID, value = Dist.CLIENT)
public final class KineticStressClientEvents {
    @SubscribeEvent
    public static void disconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        KineticStressValues.clearClient();
    }

    private KineticStressClientEvents() { }
}
