package net.quiwh.cbjs.network;

import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.kinetics.KineticStressValues;

/** One compact server snapshot on login or successful stress-setting reload. */
public final class KineticStressSync {
    private static KineticStressPacket snapshot = new KineticStressPacket(Map.of());

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").executesOn(HandlerThread.MAIN)
                .playToClient(KineticStressPacket.TYPE, KineticStressPacket.STREAM_CODEC,
                        (packet, context) -> KineticStressValues.applyClient(packet));
    }

    public static void publish(Map<Block, KineticData> rules) {
        snapshot = KineticStressPacket.fromRules(rules);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !server.getPlayerList().getPlayers().isEmpty())
            PacketDistributor.sendToAllPlayers(snapshot);
    }

    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, snapshot);
    }

    public static void serverStopped() { snapshot = new KineticStressPacket(Map.of()); }

    private KineticStressSync() { }
}
