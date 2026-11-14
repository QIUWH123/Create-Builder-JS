package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.tank.BoilerData;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;

/** Topology snapshots retain identities; stress coefficients remain live on each logical side. */
public final class BoilerEngineCapacity {
    private static final String TYPES_TAG = "CBJSEngineTypes";
    private Map<ResourceLocation, Integer> types = Map.of();
    private Map<ResourceLocation, Integer> collecting = Map.of();
    private int engineCount;
    private boolean known = true;
    private boolean refresh;
    private boolean collectingValid;

    public static BoilerEngineCapacity of(BoilerData boiler) {
        return ((BoilerEngineCapacityAccess) boiler).cbjs$engineCapacity();
    }

    public void begin() {
        collecting = new HashMap<>();
        collectingValid = true;
    }

    public void add(Block engine) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(engine);
        if (id == null) collectingValid = false;
        else collecting.merge(id, 1, Integer::sum);
    }

    public boolean finish(int attachedEngines) {
        long total = collecting.values().stream().mapToLong(Integer::longValue).sum();
        boolean complete = collectingValid && total == attachedEngines;
        Map<ResourceLocation, Integer> next = complete ? Map.copyOf(collecting) : Map.of();
        boolean changed = known != complete || engineCount != attachedEngines || !types.equals(next);
        types = next;
        engineCount = attachedEngines;
        known = complete;
        refresh = false;
        collecting = Map.of();
        return changed;
    }

    /** Retains Create's boiler-level/engine-count calculation using the installed engines' mean. */
    public double average(double nativeCapacity, int attachedEngines) {
        if (!known || engineCount != attachedEngines || attachedEngines <= 0) return nativeCapacity;
        double mean = 0;
        for (var entry : types.entrySet()) {
            double capacity = BuiltInRegistries.BLOCK.containsKey(entry.getKey())
                    ? BlockStressValues.getCapacity(BuiltInRegistries.BLOCK.get(entry.getKey())) : nativeCapacity;
            if (types.size() == 1) return capacity;
            mean += capacity * (entry.getValue() / (double) attachedEngines);
        }
        return mean;
    }

    public boolean needsRefresh() { return refresh; }

    public void write(CompoundTag boilerTag) {
        boilerTag.remove(TYPES_TAG);
        if (!known || engineCount <= 0) return;
        var list = new ListTag();
        types.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            var type = new CompoundTag();
            type.putString("Id", entry.getKey().toString());
            type.putInt("Count", entry.getValue());
            list.add(type);
        });
        boilerTag.put(TYPES_TAG, list);
    }

    public void read(CompoundTag boilerTag, int attachedEngines) {
        clear();
        if (attachedEngines <= 0) return;
        known = false;
        refresh = true;
        var list = boilerTag.getList(TYPES_TAG, Tag.TAG_COMPOUND);
        if (list.isEmpty() || list.size() > attachedEngines) return;
        var next = new HashMap<ResourceLocation, Integer>();
        long total = 0;
        for (int i = 0; i < list.size(); i++) {
            CompoundTag type = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(type.getString("Id"));
            int count = type.getInt("Count");
            if (id == null || count <= 0 || !BuiltInRegistries.BLOCK.containsKey(id)) return;
            Block block = BuiltInRegistries.BLOCK.get(id);
            if (!SteamFamilies.has(AllBlocks.STEAM_ENGINE, block.defaultBlockState())
                    || next.putIfAbsent(id, count) != null) return;
            total += count;
            if (total > attachedEngines) return;
        }
        if (total != attachedEngines) return;
        types = Map.copyOf(next);
        engineCount = attachedEngines;
        known = true;
        refresh = false;
    }

    public void clear() {
        types = Map.of();
        collecting = Map.of();
        engineCount = 0;
        known = true;
        refresh = false;
        collectingValid = false;
    }
}
