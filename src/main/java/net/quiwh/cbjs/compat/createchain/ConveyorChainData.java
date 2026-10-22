package net.quiwh.cbjs.compat.createchain;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.ChainMaterials;

/** Only nonvanilla edge materials need storage; old conveyor saves remain vanilla. */
public final class ConveyorChainData {
    public static final String TAG = "CBJSChainMaterials";
    private static final String LEGACY_TAG = "KineticJSChainMaterials";
    private final Map<BlockPos, ResourceLocation> materials = new HashMap<>();
    private long limitsRevision = Long.MIN_VALUE;
    private List<BlockPos> limitedConnections = List.of();

    public ResourceLocation material(BlockPos relativeTarget) {
        return materials.getOrDefault(relativeTarget, ChainMaterials.VANILLA);
    }

    public void put(BlockPos relativeTarget, ResourceLocation material) {
        material = ChainMaterials.normalize(material);
        if (ChainMaterials.VANILLA.equals(material)) materials.remove(relativeTarget);
        else materials.put(relativeTarget.immutable(), material);
        limitsRevision = Long.MIN_VALUE;
    }

    public void remove(BlockPos relativeTarget) {
        if (materials.remove(relativeTarget) != null) limitsRevision = Long.MIN_VALUE;
    }

    /** Rebuild only after topology/material edits or a successful kinetic-data reload. */
    public List<BlockPos> limitedConnections() {
        long revision = ChainMaterials.limitsRevision();
        if (limitsRevision != revision) {
            var rated = new java.util.ArrayList<BlockPos>();
            materials.forEach((target, material) -> {
                var limits = ChainMaterials.limits(material);
                if (limits != null && (limits.maxSpeed() != null || limits.maxNetworkStress() != null)) rated.add(target);
            });
            limitedConnections = List.copyOf(rated);
            limitsRevision = revision;
        }
        return limitedConnections;
    }

    public void write(CompoundTag tag, Set<BlockPos> activeConnections) {
        tag.remove(LEGACY_TAG);
        tag.remove(TAG);
        if (materials.isEmpty()) return;
        ListTag entries = new ListTag();
        materials.forEach((target, material) -> {
            if (!activeConnections.contains(target)) return;
            CompoundTag entry = new CompoundTag();
            entry.put("Target", NbtUtils.writeBlockPos(target));
            entry.putString("Material", material.toString());
            entries.add(entry);
        });
        if (!entries.isEmpty()) tag.put(TAG, entries);
    }

    public void read(CompoundTag tag) {
        materials.clear();
        limitsRevision = Long.MIN_VALUE;
        for (var raw : tag.getList(tag.contains(TAG) ? TAG : LEGACY_TAG, 10)) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.contains("Target", 10)) continue;
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Material"));
            if (id != null) put(NbtUtils.readBlockPos(entry.getCompound("Target")), id);
        }
    }

    public static boolean hasSavedMaterials(CompoundTag tag) {
        return tag.contains(TAG) || tag.contains(LEGACY_TAG);
    }

    public void retain(Set<BlockPos> activeConnections) {
        if (materials.keySet().retainAll(activeConnections)) limitsRevision = Long.MIN_VALUE;
    }

    public void transform(UnaryOperator<BlockPos> transform) {
        if (materials.isEmpty()) return;
        Map<BlockPos, ResourceLocation> transformed = new HashMap<>();
        materials.forEach((target, material) -> transformed.put(transform.apply(target).immutable(), material));
        materials.clear();
        materials.putAll(transformed);
        limitsRevision = Long.MIN_VALUE;
    }
}
