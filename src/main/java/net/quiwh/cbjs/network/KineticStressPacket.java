package net.quiwh.cbjs.network;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.quiwh.cbjs.api.KineticData;

/** Complete replacement snapshot; material limits and replacement drops stay on the server. */
public record KineticStressPacket(Map<ResourceLocation, Values> values) implements CustomPacketPayload {
    public static final Type<KineticStressPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cbjs", "kinetic_stress"));
    public static final StreamCodec<FriendlyByteBuf, KineticStressPacket> STREAM_CODEC = StreamCodec.of(
            (buffer, packet) -> encode(packet, buffer), KineticStressPacket::decode);

    @Override
    public Type<KineticStressPacket> type() { return TYPE; }

    private static final int MAX_ENTRIES = 65_536;

    public record Values(Double impact, Double capacity) {
        public Values {
            KineticData.validateStress("stressImpact", impact);
            KineticData.validateStress("stressCapacity", capacity);
        }
    }

    public KineticStressPacket {
        values = Map.copyOf(values);
        if (values.size() > MAX_ENTRIES) throw new IllegalArgumentException("Too many kinetic stress overrides");
    }

    public static KineticStressPacket fromRules(Map<Block, KineticData> rules) {
        var values = new HashMap<ResourceLocation, Values>();
        rules.forEach((block, rule) -> {
            if (rule.stressImpact() == null && rule.stressCapacity() == null) return;
            var id = BuiltInRegistries.BLOCK.getKey(block);
            if (id != null) values.put(id, new Values(rule.stressImpact(), rule.stressCapacity()));
        });
        return new KineticStressPacket(values);
    }

    public static void encode(KineticStressPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.values.size());
        packet.values.forEach((id, value) -> {
            buffer.writeResourceLocation(id);
            int fields = (value.impact == null ? 0 : 1) | (value.capacity == null ? 0 : 2);
            buffer.writeByte(fields);
            if (value.impact != null) buffer.writeDouble(value.impact);
            if (value.capacity != null) buffer.writeDouble(value.capacity);
        });
    }

    public static KineticStressPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid kinetic stress override count");
        var values = new HashMap<ResourceLocation, Values>();
        for (int i = 0; i < count; i++) {
            var id = buffer.readResourceLocation();
            int fields = buffer.readUnsignedByte();
            if (fields == 0 || fields > 3) throw new IllegalArgumentException("Invalid kinetic stress override fields");
            var value = new Values((fields & 1) == 0 ? null : buffer.readDouble(),
                    (fields & 2) == 0 ? null : buffer.readDouble());
            if (values.put(id, value) != null) throw new IllegalArgumentException("Duplicate kinetic stress override: " + id);
        }
        return new KineticStressPacket(values);
    }
}
