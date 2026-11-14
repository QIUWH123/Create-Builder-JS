package net.quiwh.cbjs.drives;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/** Immutable topology. The dimension's saved data, rather than copied block NBT, owns it. */
public final class DriveLoop {
    public static final int MAX_NODES = 16;
    private final UUID id;
    private final DriveKind kind;
    private final ResourceLocation material;
    private final Direction.Axis axis;
    private final List<DriveNode> nodes;
    private final int paid;
    private final DrivePath path;
    private final List<BlockPos> chunks;

    public DriveLoop(UUID id, DriveKind kind, ResourceLocation material, Direction.Axis axis,
                     List<DriveNode> nodes, int paid) {
        this.id = Objects.requireNonNull(id, "id");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.material = Objects.requireNonNull(material, "material");
        this.axis = Objects.requireNonNull(axis, "axis");
        this.nodes = List.copyOf(nodes);
        if (this.nodes.size() < 2 || this.nodes.size() > MAX_NODES)
            throw new IllegalArgumentException("A drive needs between 2 and 16 cogwheels");
        for (DriveNode node : this.nodes) {
            BlockPos pos = node.pos();
            if (Math.abs((long) pos.getX()) > 30_000_000 || Math.abs((long) pos.getZ()) > 30_000_000
                    || Math.abs((long) pos.getY()) > 2048)
                throw new IllegalArgumentException("Drive node is outside world bounds");
        }
        path = DrivePath.create(axis, this.nodes);
        // Chunk-coordinate pairs, cached once; tick/propagation checks never resample geometry.
        var chunks = new LinkedHashSet<BlockPos>();
        for (var point : path.samples()) {
            BlockPos block = BlockPos.containing(point);
            chunks.add(new BlockPos(block.getX() >> 4, 0, block.getZ() >> 4));
        }
        this.chunks = List.copyOf(chunks);
        if (paid < 0 || paid > cost())
            throw new IllegalArgumentException("Drive payment must be between zero and its material cost");
        this.paid = paid;
    }

    public UUID id() { return id; }
    public DriveKind kind() { return kind; }
    public ResourceLocation material() { return material; }
    public Direction.Axis axis() { return axis; }
    public List<DriveNode> nodes() { return nodes; }
    public int paid() { return paid; }
    public DrivePath path() { return path; }
    public int cost() { return (int) Math.ceil(path.length()); }
    public boolean active(ServerLevel level) { return DriveManager.isActive(level, this); }
    public boolean chunksLoaded(ServerLevel level) {
        for (BlockPos chunk : chunks) if (!level.hasChunk(chunk.getX(), chunk.getZ())) return false;
        return true;
    }

    public CompoundTag serialize(CompoundTag tag) {
        tag.putInt("Version", 1);
        tag.putUUID("Id", id);
        tag.putString("Kind", kind.name());
        tag.putString("Material", material.toString());
        tag.putString("Axis", axis.getName());
        tag.putInt("Paid", paid);
        ListTag list = new ListTag();
        for (DriveNode node : nodes) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Pos", node.pos().asLong());
            entry.putDouble("Radius", node.radius());
            list.add(entry);
        }
        tag.put("Nodes", list);
        return tag;
    }

    /** Invalid or oversized entries never create links or refundable items. */
    public static DriveLoop read(CompoundTag tag) {
        try {
            if (tag.getInt("Version") != 1 || !tag.hasUUID("Id")
                    || !tag.contains("Paid", Tag.TAG_INT)) return null;
            ListTag list = tag.getList("Nodes", Tag.TAG_COMPOUND);
            if (list.size() < 2 || list.size() > MAX_NODES) return null;
            ResourceLocation material = ResourceLocation.tryParse(tag.getString("Material"));
            if (material == null) return null;
            Direction.Axis axis = Direction.Axis.valueOf(tag.getString("Axis").toUpperCase(java.util.Locale.ROOT));
            List<DriveNode> nodes = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                CompoundTag entry = list.getCompound(i);
                if (!entry.contains("Pos", Tag.TAG_LONG) || !entry.contains("Radius", Tag.TAG_DOUBLE)) return null;
                nodes.add(new DriveNode(BlockPos.of(entry.getLong("Pos")), entry.getDouble("Radius")));
            }
            return new DriveLoop(tag.getUUID("Id"), DriveKind.valueOf(tag.getString("Kind")),
                    material, axis, nodes, tag.getInt("Paid"));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }
}
