package net.quiwh.cbjs.drives;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Single authoritative owner prevents unloaded or copied block entities refunding a loop twice. */
public final class DriveSavedData extends SavedData {
    public static final String FILE_NAME = "cbjs_cogwheel_drives";
    private static final int MAX_LOOPS = 16_384;
    private static final Factory<DriveSavedData> FACTORY = new Factory<>(DriveSavedData::new, DriveSavedData::load);
    private final Map<UUID, DriveLoop> loops = new LinkedHashMap<>();
    private final Map<Long, UUID> positions = new HashMap<>();
    private final Set<UUID> active = new HashSet<>();
    private final Set<UUID> disconnecting = new HashSet<>();
    private final Map<UUID, Long> checkedAt = new HashMap<>();

    public DriveSavedData() { }

    public static DriveSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, FILE_NAME);
    }

    public DriveLoop get(UUID id) { return loops.get(id); }
    public DriveLoop loopAt(BlockPos pos) {
        UUID id = positions.get(pos.asLong());
        return id == null ? null : loops.get(id);
    }
    public Collection<DriveLoop> loops() { return Collections.unmodifiableCollection(loops.values()); }

    public void add(DriveLoop loop) {
        if (loops.size() >= MAX_LOOPS || loops.containsKey(loop.id()))
            throw new IllegalArgumentException("Drive loop limit or duplicate identity");
        for (DriveNode node : loop.nodes())
            if (positions.containsKey(node.pos().asLong()))
                throw new IllegalArgumentException("Cogwheel already belongs to a drive");
        loops.put(loop.id(), loop);
        for (DriveNode node : loop.nodes()) positions.put(node.pos().asLong(), loop.id());
        setDirty();
    }

    public DriveLoop remove(UUID id) {
        DriveLoop loop = loops.remove(id);
        if (loop == null) return null;
        for (DriveNode node : loop.nodes()) positions.remove(node.pos().asLong(), id);
        active.remove(id);
        checkedAt.remove(id);
        setDirty();
        return loop;
    }

    boolean isActive(UUID id) { return active.contains(id); }
    boolean beginDisconnect(UUID id) { return loops.containsKey(id) && disconnecting.add(id); }
    void endDisconnect(UUID id) { disconnecting.remove(id); }
    void active(UUID id, boolean enabled) {
        if (enabled) active.add(id); else active.remove(id);
    }
    void recheck(UUID id) { checkedAt.remove(id); }
    boolean needsCheck(UUID id, long tick) {
        Long previous = checkedAt.put(id, tick);
        return previous == null || previous.longValue() != tick;
    }

    public static DriveSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DriveSavedData data = new DriveSavedData();
        if (tag.getInt("Version") != 1) return data;
        ListTag list = tag.getList("Loops", Tag.TAG_COMPOUND);
        for (int i = 0, count = Math.min(list.size(), MAX_LOOPS); i < count; i++) {
            DriveLoop loop = DriveLoop.read(list.getCompound(i));
            if (loop == null) continue;
            try { data.add(loop); }
            catch (IllegalArgumentException ignored) { /* First complete indexed loop owns each position. */ }
        }
        data.setDirty(false);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (DriveLoop loop : loops.values()) list.add(loop.serialize(new CompoundTag()));
        tag.putInt("Version", 1);
        tag.put("Loops", list);
        return tag;
    }
}
