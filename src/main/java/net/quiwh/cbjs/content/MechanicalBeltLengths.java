package net.quiwh.cbjs.content;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;

/** A material's explicit cap counts occupied blocks, including both end pulleys. */
public final class MechanicalBeltLengths {
    private static final ThreadLocal<BlockState> SELECTED = new ThreadLocal<>();
    private static final Map<Level, DeferredInitialization> DEFERRED = new WeakHashMap<>();

    private static final class DeferredInitialization {
        long tick = Long.MIN_VALUE;
        final java.util.Set<BlockPos> positions = new HashSet<>();
    }

    public static Integer configured(BlockState state) {
        return state != null && state.getBlock() instanceof ScriptedMechanicalBeltBlock belt
                ? belt.sourceBuilder().maxLength == null ? BeltConnectorItem.maxLength() : belt.sourceBuilder().maxLength : null;
    }

    /**
     * Returns whether this belt explicitly selected Create Builder JS length geometry.
     *
     * An omitted maxLength is intentionally different from an explicit value equal to
     * Create's current cap: omitted belts must continue through Create's native
     * Euclidean checks. Integer.MAX_VALUE is the explicit
     * noMaxLength() mode and therefore belongs to the custom path as well.
     */
    public static boolean usesCustomGeometry(BlockState state) {
        return state != null && state.getBlock() instanceof ScriptedMechanicalBeltBlock belt
                && belt.sourceBuilder().maxLength != null;
    }

    /** Returns only an explicitly configured Create Builder JS cap; null means inherit Create. */
    public static Integer customConfigured(BlockState state) {
        return usesCustomGeometry(state)
                ? ((ScriptedMechanicalBeltBlock) state.getBlock()).sourceBuilder().maxLength
                : null;
    }

    public static BlockState material(Item item) {
        return item instanceof ScriptedBeltConnectorItem connector ? connector.getBlock().defaultBlockState() : null;
    }

    public static Integer current() { return customConfigured(SELECTED.get()); }

    /** Safety checks apply to inherited materials too, independently of length geometry. */
    public static boolean hasSelectedBelt() { return SELECTED.get() != null; }

    public static <T> T withBelt(BlockState state, Supplier<T> action) {
        BlockState previous = SELECTED.get();
        // Only explicit and unlimited registrations enter the custom path.  An omitted
        // maxLength must leave Create's native distance behavior untouched.
        BlockState selected = state != null && state.getBlock() instanceof ScriptedMechanicalBeltBlock ? state : null;
        if (selected == null) SELECTED.remove();
        else SELECTED.set(selected);
        try { return action.get(); }
        finally {
            if (previous == null) SELECTED.remove();
            else SELECTED.set(previous);
        }
    }

    public static boolean within(Vec3i first, Vec3i second, int cap) {
        return within(first, second, (long) cap);
    }

    public static boolean within(Vec3i first, Vec3i second, long cap) {
        return occupiedLength(first, second) <= cap;
    }

    public static long occupiedLength(Vec3i first, Vec3i second) {
        long dx = Math.abs((long)first.getX() - second.getX());
        long dy = Math.abs((long)first.getY() - second.getY());
        long dz = Math.abs((long)first.getZ() - second.getZ());
        return Math.max(dx, Math.max(dy, dz)) + 1;
    }

    public static boolean allows(BlockState state, int length) {
        Integer cap = configured(state);
        if (cap == null) return true;
        if (usesCustomGeometry(state)) return length <= cap;
        // Saved chains store their occupied count. Recover the endpoint distance for
        // inherited materials instead of treating Create's radius as a block count.
        long distance = Math.max(0L, (long) length - 1);
        BeltSlope slope = state.getValue(BeltBlock.SLOPE);
        int axes = slope == BeltSlope.UPWARD || slope == BeltSlope.DOWNWARD ? 2 : 1;
        return (double) distance * distance * axes < (double) cap * cap;
    }

    /** Direct custom construction must obey the same policy as connector selection. */
    public static boolean allowsConnection(BlockState state, Vec3i first, Vec3i second) {
        Integer cap = configured(state);
        return cap == null || (usesCustomGeometry(state)
                ? within(first, second, cap) : first.closerThan(second, cap.doubleValue()));
    }

    /** Native connector budgets must cover the selected endpoints, rather than stop at 1000. */
    public static int endpointBudget(Vec3i first, Vec3i second) {
        return (int) Math.min(Integer.MAX_VALUE, occupiedLength(first, second));
    }

    /** Walk only the existing, loaded family. A malformed loop cannot spin forever or load chunks. */
    public static List<BlockPos> scanChain(LevelAccessor level, BlockPos start, boolean forward) {
        if (!level.hasChunk(start.getX() >> 4, start.getZ() >> 4)) return List.of();
        BlockState family = level.getBlockState(start);
        List<BlockPos> chain = new ArrayList<>();
        var visited = new HashSet<BlockPos>();
        BlockPos current = start;
        while (current != null && level.hasChunk(current.getX() >> 4, current.getZ() >> 4)
                && visited.add(current)) {
            BlockState state = level.getBlockState(current);
            if (state.getBlock() != family.getBlock()) break;
            chain.add(current.immutable());
            current = BeltBlock.nextSegmentPosition(state, current, forward);
        }
        return chain;
    }

    /** Initialization must defer rather than turn a loaded prefix into a shorter belt. */
    public static boolean initializationLoaded(Level level, BlockPos pos) {
        DeferredInitialization deferred;
        synchronized (DEFERRED) {
            deferred = DEFERRED.computeIfAbsent(level, ignored -> new DeferredInitialization());
        }
        long tick = level.getGameTime();
        if (deferred.tick != tick) {
            deferred.positions.clear();
            deferred.tick = tick;
        }
        if (deferred.positions.contains(pos)) return false;
        List<BlockPos> backward = scanChain(level, pos, false);
        List<BlockPos> forward = scanChain(level, pos, true);
        boolean loaded = endsLoaded(level, backward, false) && endsLoaded(level, forward, true);
        if (!loaded) {
            // Each uninitialized member retries every tick. Share the failed scan over this
            // connected loaded run, so a missing chunk costs O(n), rather than O(n squared).
            deferred.positions.addAll(backward);
            deferred.positions.addAll(forward);
        }
        return loaded;
    }

    private static boolean endsLoaded(LevelAccessor level, List<BlockPos> chain, boolean forward) {
        if (chain.isEmpty()) return false;
        BlockPos last = chain.get(chain.size() - 1);
        BlockPos next = BeltBlock.nextSegmentPosition(level.getBlockState(last), last, forward);
        return next == null || level.hasChunk(next.getX() >> 4, next.getZ() >> 4);
    }

    public static int initializationBudget(LevelAccessor level, BlockPos start) {
        int size = scanChain(level, start, false).size();
        return size == Integer.MAX_VALUE ? size : size + 1;
    }

    private MechanicalBeltLengths() { }
}
