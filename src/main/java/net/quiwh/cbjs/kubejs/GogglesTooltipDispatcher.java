package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import java.lang.ref.WeakReference;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

/** Run dynamic script text once per client tick, retaining native overlay rendering. */
public final class GogglesTooltipDispatcher {
    private static Cache cache;
    private static WeakReference<List<Component>> activeLines = new WeakReference<>(null);
    private static GogglesOverlayOptions activeOptions;

    public static boolean apply(Level level, BlockPos pos, Player player, Direction face,
                                boolean shift, long tick, List<Component> lines) {
        EventHandler handler = CBJSEvents.GOGGLES_TOOLTIP;
        if (!handler.hasListeners()) { cache = null; activeOptions = null; return false; }
        BlockState state = level.getBlockState(pos);
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (!handler.hasListeners(id)) { activeOptions = null; return false; }
        var manager = ScriptType.CLIENT.manager.get();
        Object context = manager == null ? null : manager.context;
        Cache previous = cache;
        if (previous != null && previous.matches(level, pos, player, face, state, shift, tick, context)) {
            lines.clear(); lines.addAll(previous.lines());
            activeLines = new WeakReference<>(lines); activeOptions = previous.options();
            return true;
        }
        var event = new GogglesTooltipEventJS(level, pos, player, face, shift, lines);
        handler.post(ScriptType.CLIENT, id, event);
        activeLines = new WeakReference<>(lines); activeOptions = event.options();
        cache = new Cache(new WeakReference<>(level), pos.immutable(), new WeakReference<>(player), face,
                state, shift, tick, new WeakReference<>(context), List.copyOf(lines), activeOptions);
        return true;
    }

    public static void invalidate(EventHandler handler, ScriptType type) {
        if (handler == CBJSEvents.GOGGLES_TOOLTIP && type == ScriptType.CLIENT) {
            cache = null; activeOptions = null; activeLines.clear();
        }
    }
    @dev.latvian.mods.rhino.util.HideFromJS
    public static GogglesOverlayOptions options(List<?> lines) { return activeLines.get() == lines ? activeOptions : null; }

    private record Cache(WeakReference<Level> level, BlockPos pos, WeakReference<Player> player,
                         Direction face, BlockState state, boolean shift, long tick,
                         WeakReference<Object> context, List<Component> lines, GogglesOverlayOptions options) {
        boolean matches(Level level, BlockPos pos, Player player, Direction face, BlockState state,
                        boolean shift, long tick, Object context) {
            return this.level.get() == level && this.pos.equals(pos) && this.player.get() == player
                    && this.face == face && this.state == state && this.shift == shift && this.tick == tick
                    && this.context.get() == context;
        }
    }
    private GogglesTooltipDispatcher() { }
}
