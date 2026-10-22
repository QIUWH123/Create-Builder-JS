package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import dev.latvian.mods.kubejs.bindings.TextWrapper;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/** Client-visible inspection context; text is merged into Create's goggles overlay. */
public final class GogglesTooltipEventJS extends LevelEventJS {
    private final Level level;
    private final BlockPos pos;
    private final Player player;
    private final Direction face;
    private final boolean shift;
    private final String blockId;
    private final List<Component> lines;
    private final TreeMap<Integer, GogglesOverlayOptions.Row> rows = new TreeMap<>();
    private Integer x, y, background, borderTop, borderBottom;
    private int offsetX, offsetY, lineSpacing = 1;
    private double scale = 1;
    private boolean noBackground;

    public GogglesTooltipEventJS(Level level, BlockPos pos, Player player, Direction face,
                                boolean shift, List<Component> lines) {
        this.level = level;
        this.pos = pos.immutable();
        this.player = player;
        this.face = face;
        this.shift = shift;
        this.lines = lines;
        blockId = Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock())).toString();
    }

    @Override public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockContainerJS getBlock() { return new BlockContainerJS(level, pos); }
    public Player getPlayer() { return player; }
    public String getBlockId() { return blockId; }
    public Direction getFace() { return face; }
    public boolean getShift() { return shift; }
    public boolean getIsSneaking() { return shift; }
    public List<Component> getLines() { return List.copyOf(lines); }
    public void add(Component text) { lines.add(Objects.requireNonNull(text, "Goggles text cannot be null")); }
    public void addAll(List<Component> text) {
        // Rhino converts the outer Java list but may leave individual string values raw.
        for (Object line : text) add(TextWrapper.of(Objects.requireNonNull(line, "Goggles text cannot be null")));
    }
    public int getLineCount() { return lines.size(); }
    public Component getLine(int index) { return index >= 0 && index < lines.size() ? lines.get(index) : null; }
    /** Invalid indexes return false, so optional native lines can be edited safely. */
    public boolean setLine(int index, Component text) {
        if (index < 0 || index >= lines.size()) return false;
        lines.set(index, Objects.requireNonNull(text, "Goggles text cannot be null"));
        rows.remove(index);
        return true;
    }
    public boolean insertLine(int index, Component text) {
        if (index < 0 || index > lines.size()) return false;
        lines.add(index, Objects.requireNonNull(text, "Goggles text cannot be null"));
        shiftRows(index, 1);
        return true;
    }
    public boolean removeLine(int index) {
        if (index < 0 || index >= lines.size()) return false;
        lines.remove(index); rows.remove(index); shiftRows(index + 1, -1); return true;
    }
    private void shiftRows(int from, int delta) {
        var changed = new TreeMap<>(rows.tailMap(from, true));
        changed.keySet().forEach(rows::remove);
        changed.forEach((index, row) -> rows.put(index + delta, row));
    }
    public void clear() { lines.clear(); rows.clear(); }
    public void addItem(ItemStack item) { addItem(item, Objects.requireNonNull(item, "Goggles item cannot be null").getHoverName()); }
    public void addItem(ItemStack item, Component text) {
        Objects.requireNonNull(item, "Goggles item cannot be null");
        Objects.requireNonNull(text, "Goggles text cannot be null");
        rows.put(lines.size(), new GogglesOverlayOptions.Row(item, null, 0, 0, 0)); add(text);
    }
    public void addFluid(FluidStack fluid) { addFluid(fluid, Objects.requireNonNull(fluid, "Goggles fluid cannot be null").getDisplayName()); }
    public void addFluid(FluidStack fluid, Component text) {
        Objects.requireNonNull(fluid, "Goggles fluid cannot be null");
        Objects.requireNonNull(text, "Goggles text cannot be null");
        rows.put(lines.size(), new GogglesOverlayOptions.Row(null, fluid, 0, 0, 0)); add(text);
    }
    public void addProgress(double value, double maximum, Component text) { addProgress(value, maximum, text, 120, 0xff55aaff); }
    public void addProgress(double value, double maximum, Component text, long color) { addProgress(value, maximum, text, 120, color); }
    public void addProgress(double value, double maximum, Component text, int width, long color) {
        if (!Double.isFinite(value) || !Double.isFinite(maximum) || maximum <= 0)
            throw new IllegalArgumentException("Progress requires a finite value and a positive finite maximum");
        if (width < 1 || width > 4096) throw new IllegalArgumentException("Progress width must be between 1 and 4096");
        Objects.requireNonNull(text, "Goggles text cannot be null");
        rows.put(lines.size(), new GogglesOverlayOptions.Row(null, null, Math.max(0, Math.min(1, value / maximum)), width, argb(color))); add(text);
    }
    public GogglesTooltipEventJS position(int x, int y) { this.x = x; this.y = y; return this; }
    public GogglesTooltipEventJS offset(int x, int y) { offsetX = x; offsetY = y; return this; }
    public GogglesTooltipEventJS scale(double value) {
        if (!Double.isFinite(value) || value <= 0 || value > 16) throw new IllegalArgumentException("Goggles scale must be greater than zero and at most 16");
        scale = value; return this;
    }
    public GogglesTooltipEventJS lineSpacing(int pixels) {
        if (pixels < 0 || pixels > 128) throw new IllegalArgumentException("Goggles line spacing must be between 0 and 128");
        lineSpacing = pixels; return this;
    }
    public GogglesTooltipEventJS background(long color) { background = argb(color); noBackground = false; return this; }
    public GogglesTooltipEventJS border(long topArgb, long bottomArgb) { borderTop = argb(topArgb); borderBottom = argb(bottomArgb); noBackground = false; return this; }
    private static int argb(long value) {
        if (value < Integer.MIN_VALUE || value > 0xffffffffL)
            throw new IllegalArgumentException("Goggles colors must be a 32-bit ARGB value");
        return (int)value;
    }
    public GogglesTooltipEventJS noBackground() { noBackground = true; return this; }
    public GogglesKineticInfo getKinetic() { return GogglesKineticInfo.of(level.getBlockEntity(pos)); }
    public GogglesStorageInfo getStorage() { return GogglesStorageInfo.of(level.getBlockEntity(pos)); }
    @HideFromJS public GogglesOverlayOptions options() {
        return new GogglesOverlayOptions(rows, x, y, offsetX, offsetY, scale, lineSpacing, background, borderTop, borderBottom, noBackground);
    }
}
