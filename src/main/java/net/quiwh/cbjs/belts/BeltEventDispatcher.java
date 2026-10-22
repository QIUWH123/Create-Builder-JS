package net.quiwh.cbjs.belts;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.server.ServerScriptManager;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.*;

/** Native completed-action hooks queue detached reports; scripts run after transport iteration. */
public final class BeltEventDispatcher {
    private static Batch pending;
    private static long generation;
    private static final ThreadLocal<ProcessingContext> PROCESSOR = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> MIGRATING = new ThreadLocal<>();
    private BeltEventDispatcher() { }
    public static boolean listens(EventHandler event, BeltBlockEntity belt) {
        return event.hasListeners() && belt != null && belt.getLevel() != null
                && !belt.getLevel().isClientSide && !belt.isVirtual()
                && BeltEventFilters.hasListeners(event, belt.getBlockState().getBlock());
    }
    public static boolean observesChanges(BeltBlockEntity belt) {
        return listens(CBJSEvents.BELT_ITEM_EXITED, belt) || listens(CBJSEvents.BELT_ITEM_PROCESSED, belt);
    }
    public static boolean needsProcessingContext() {
        // Exit-only observers must distinguish consumed ingredients from external extraction too.
        return CBJSEvents.BELT_ITEM_PROCESSED.hasListeners() || CBJSEvents.BELT_ITEM_EXITED.hasListeners();
    }
    public static void entered(BeltBlockEntity belt, TransportedItemStack transported) {
        if (!listens(CBJSEvents.BELT_ITEM_ENTERED, belt) || Boolean.TRUE.equals(MIGRATING.get())
                || transported.stack.isEmpty()) return;
        BlockPos pos = segmentPos(belt, transported.insertedAt);
        Direction from = transported.insertedFrom;
        // Native addItem also serves capability insertion; it does not identify the source owner.
        transfer(CBJSEvents.BELT_ITEM_ENTERED, belt, pos, transported.stack, "insertion",
                null, pos, from);
    }
    public static void exited(BeltBlockEntity belt, float position, ItemStack moved, String reason,
                              @Nullable BlockPos destination, @Nullable Direction direction) {
        if (!listens(CBJSEvents.BELT_ITEM_EXITED, belt) || moved.isEmpty()) return;
        BlockPos pos = segmentPos(belt, (int) position);
        transfer(CBJSEvents.BELT_ITEM_EXITED, belt, pos, moved, reason, pos, destination, direction);
    }
    public static void lost(BeltBlockEntity belt, float position, ItemStack before, ItemStack after,
                            String reason, @Nullable BlockPos destination, @Nullable Direction direction) {
        if (!listens(CBJSEvents.BELT_ITEM_EXITED, belt) || before.isEmpty()) return;
        int remaining = !after.isEmpty() && ItemStack.isSameItemSameTags(before, after) ? after.getCount() : 0;
        int moved = before.getCount() - remaining;
        if (moved > 0) {
            ItemStack copy = before.copy();
            copy.setCount(moved);
            exited(belt, position, copy, reason, destination, direction);
        }
    }
    private static void transfer(EventHandler event, BeltBlockEntity belt, BlockPos pos, ItemStack moved,
                                 String reason, BlockPos source, BlockPos destination, Direction direction) {
        enqueue(event, new BeltItemTransferEventJS(belt.getLevel(), pos, belt.getBlockPos(),
                belt.getBlockState().getBlock(), moved, reason, source, destination, direction));
    }
    public static boolean pushMigration() {
        boolean previous = Boolean.TRUE.equals(MIGRATING.get());
        MIGRATING.set(Boolean.TRUE);
        return previous;
    }
    public static void popMigration(boolean previous) {
        if (previous) MIGRATING.set(Boolean.TRUE); else MIGRATING.remove();
    }
    /** Called at the filling mutation boundary, after native unchanged/held-tick gates. */
    public static void captureProcessorInput(ItemStack stack) {
        if (!CBJSEvents.BELT_ITEM_PROCESSED.hasListeners()) return;
        ProcessingContext context = PROCESSOR.get();
        if (context != null && context.transported != null && context.before == null
                && context.transported.stack == stack && listens(CBJSEvents.BELT_ITEM_PROCESSED, context.belt))
            PROCESSOR.set(new ProcessingContext(context.level, context.pos, context.transported, context.belt, stack.copy()));
    }
    /** Snapshot a native callback result; doNothing/identical results are never actions. */
    @Nullable public static ProcessingChange capture(ItemStack before, float position, TransportedResult result) {
        return capture(before, position, result, null);
    }
    @Nullable public static ProcessingChange capture(ItemStack before, float position, TransportedResult result,
                                                       @Nullable TransportedItemStack transported) {
        ProcessingContext context = PROCESSOR.get();
        if (context != null && context.transported == transported && context.before != null)
            before = context.before;
        if (result == null || result.didntChangeFrom(before)) return null;
        List<ItemStack> outputs = result.getOutputs().stream().map(t -> t.stack.copy()).toList();
        ItemStack held = result.hasHeldOutput() ? result.getHeldOutput().stack.copy() : ItemStack.EMPTY;
        return new ProcessingChange(before.copy(), position, outputs, held, PROCESSOR.get());
    }
    /** Called after native replacement output/removal queues committed one result. */
    public static void committed(BeltBlockEntity belt, ProcessingChange change) {
        if (change.processor != null) {
            if (!listens(CBJSEvents.BELT_ITEM_PROCESSED, belt)) return;
            ProcessingContext processor = change.processor;
            String id = ForgeRegistries.BLOCKS.getKey(processor.level.getBlockState(processor.pos).getBlock()).toString();
            enqueue(CBJSEvents.BELT_ITEM_PROCESSED, new BeltItemProcessedEventJS(belt.getLevel(),
                    segmentPos(belt, (int) change.position), belt.getBlockPos(), belt.getBlockState().getBlock(),
                    change.before, change.outputs, change.held, processor.pos, id));
        } else {
            // Generic handlers also service players/chutes. Only pure removals are known exits;
            // unknown conversions cannot safely be called processing or a transfer.
            if (!change.held.isEmpty() || change.outputs.stream().anyMatch(s -> !s.isEmpty())) return;
            exited(belt, change.position, change.before, "extraction", null, null);
        }
    }
    @Nullable public static ProcessingContext pushProcessor(Level level, BlockPos pos) {
        ProcessingContext previous = PROCESSOR.get();
        PROCESSOR.set(new ProcessingContext(level, pos.immutable()));
        return previous;
    }
    @Nullable public static ProcessingContext pushProcessor(Level level, BlockPos pos,
            TransportedItemStack transported, @Nullable TransportedItemStackHandlerBehaviour handler) {
        ProcessingContext previous = PROCESSOR.get();
        BeltBlockEntity belt = handler != null && handler.blockEntity instanceof BeltBlockEntity b
                && listens(CBJSEvents.BELT_ITEM_PROCESSED, b) ? b : null;
        PROCESSOR.set(new ProcessingContext(level, pos.immutable(), belt == null ? null : transported, belt, null));
        return previous;
    }
    public static void popProcessor(@Nullable ProcessingContext previous) {
        if (previous == null) PROCESSOR.remove(); else PROCESSOR.set(previous);
    }
    public static BlockPos segmentPos(BeltBlockEntity belt, int segment) {
        return BeltHelper.getPositionForOffset(belt, Math.max(0, Math.min(Math.max(0, belt.beltLength - 1), segment)));
    }
    private static Object scope() {
        return ServerScriptManager.instance == null ? null : ServerScriptManager.instance.topLevelScope;
    }
    private static void enqueue(EventHandler handler, BeltItemEventJS event) {
        if (pending == null || !pending.active()) pending = new Batch(scope());
        pending.reports.add(new Report(handler, event));
    }
    public static void flush() {
        Batch current = pending;
        pending = null;
        if (current == null || !current.active()) return;
        for (Report report : current.reports) {
            if (!current.active()) break;
            try {
                report.handler.post(report.event, net.minecraft.resources.ResourceLocation.parse(report.event.getBlockId()));
            } catch (RuntimeException failure) {
                // KubeJS may clear its own handler array inside a callback during reload.
                // That stale dispatch cannot be resumed; unrelated failures still propagate.
                if (current.active()) throw failure;
                break;
            }
        }
    }
    public static void invalidate(EventHandler event, ScriptType type) {
        if (type == ScriptType.SERVER && BeltEventFilters.handles(event)) clear();
    }
    public static void clear() { pending = null; generation++; PROCESSOR.remove(); MIGRATING.remove(); }
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) flush();
    }
    public static void onServerStopped(ServerStoppedEvent event) { clear(); }
    public record ProcessingContext(Level level, BlockPos pos, @Nullable TransportedItemStack transported,
                                    @Nullable BeltBlockEntity belt, @Nullable ItemStack before) {
        public ProcessingContext(Level level, BlockPos pos) { this(level, pos, null, null, null); }
    }
    public record ProcessingChange(ItemStack before, float position, List<ItemStack> outputs,
                                   ItemStack held, @Nullable ProcessingContext processor) { }
    private record Report(EventHandler handler, BeltItemEventJS event) { }
    private static final class Batch {
        final Object scope;
        final long generation = BeltEventDispatcher.generation;
        final List<Report> reports = new ArrayList<>();
        Batch(Object scope) { this.scope = scope; }
        boolean active() { return generation == BeltEventDispatcher.generation && scope == scope(); }
    }
}
