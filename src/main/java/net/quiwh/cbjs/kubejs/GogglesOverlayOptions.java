package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.rhino.util.HideFromJS;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/** Immutable client-tick presentation snapshot, with no reference to the inspected world. */
@HideFromJS
public record GogglesOverlayOptions(Map<Integer, Row> rows, Integer x, Integer y, int offsetX, int offsetY,
                                    double scale, int lineSpacing, Integer background, Integer borderTop,
                                    Integer borderBottom, boolean noBackground) {
    public GogglesOverlayOptions { rows = Map.copyOf(rows); }
    public boolean customRows() { return !rows.isEmpty() || lineSpacing != 1; }
    public boolean customLayout() { return customRows() || x != null || y != null || offsetX != 0 || offsetY != 0 || scale != 1; }
    public record Row(ItemStack item, FluidStack fluid, double progress, int width, int color) {
        public Row {
            item = item == null ? ItemStack.EMPTY : item.copy();
            fluid = fluid == null ? FluidStack.EMPTY : fluid.copy();
        }
        @Override public ItemStack item() { return item.copy(); }
        @Override public FluidStack fluid() { return fluid.copy(); }
        public boolean isProgress() { return width > 0; }
        public int height() { return isProgress() ? 20 : 16; }
    }
}
