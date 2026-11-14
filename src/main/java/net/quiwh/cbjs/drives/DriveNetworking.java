package net.quiwh.cbjs.drives;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

public final class DriveNetworking {
    public record Action(BlockPos member, boolean remove) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.parse("cbjs:drive_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> CODEC = StreamCodec.of(
                (buf, packet) -> { buf.writeBlockPos(packet.member); buf.writeBoolean(packet.remove); },
                buf -> new Action(buf.readBlockPos(), buf.readBoolean()));
        @Override public Type<Action> type() { return TYPE; }
    }
    public record Selection(DriveKind kind, ResourceLocation material, List<BlockPos> nodes) implements CustomPacketPayload {
        public Selection { nodes = List.copyOf(nodes); if (nodes.size() > 16) throw new IllegalArgumentException("Too many drive nodes"); }
        public static final Type<Selection> TYPE = new Type<>(ResourceLocation.parse("cbjs:drive_selection"));
        public static final StreamCodec<FriendlyByteBuf, Selection> CODEC = StreamCodec.of((buf, packet) -> {
            buf.writeEnum(packet.kind); buf.writeResourceLocation(packet.material); buf.writeVarInt(packet.nodes.size());
            packet.nodes.forEach(buf::writeBlockPos);
        }, buf -> {
            DriveKind kind = buf.readEnum(DriveKind.class);
            ResourceLocation material = buf.readResourceLocation();
            int size = buf.readVarInt();
            if (size < 0 || size > 16) throw new IllegalArgumentException("Invalid drive node count");
            var nodes = new ArrayList<BlockPos>(size);
            for (int i = 0; i < size; i++) nodes.add(buf.readBlockPos());
            return new Selection(kind, material, nodes);
        });
        @Override public Type<Selection> type() { return TYPE; }
    }
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").executesOn(HandlerThread.MAIN);
        registrar.playToServer(Action.TYPE, Action.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) DriveInteraction.act(player, packet.member, packet.remove);
        });
        registrar.playToClient(Selection.TYPE, Selection.CODEC,
                (packet, context) -> net.quiwh.cbjs.client.StandaloneDriveClient.selection(packet));
    }
    public static void selection(ServerPlayer player, DriveKind kind, ResourceLocation material, List<BlockPos> nodes) {
        PacketDistributor.sendToPlayer(player, new Selection(kind, material, nodes));
    }
    private DriveNetworking() {}
}
