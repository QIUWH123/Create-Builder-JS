package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.quiwh.cbjs.kubejs.GogglesOverlayOptions;
import net.quiwh.cbjs.kubejs.GogglesTooltipDispatcher;

/** Rich rows are drawn only inside Create's existing overlay visibility/fade lifecycle. */
public final class GogglesOverlayRenderer {
    private static GogglesOverlayOptions cachedOptions;
    private static Font cachedFont;
    private static int cachedScreenWidth;
    private static Layout cachedLayout;
    /** Called before Create renders its header icon, so the icon follows the same layout transform. */
    public static int[] position(List<?> lines, int x, int y, int screenWidth, int screenHeight, Font font) {
        GogglesOverlayOptions options = GogglesTooltipDispatcher.options(lines);
        if (options == null || !options.customLayout()) return new int[]{x, y};
        var layout = layout(lines, font, options, screenWidth);
        return resolvePosition(x, y, screenWidth, screenHeight, layout.width(), layout.height(), options);
    }
    public static int[] resolvePosition(int x, int y, int screenWidth, int screenHeight, int width, int height, GogglesOverlayOptions options) {
        int chosenX = (options.x() == null ? x : options.x()) + options.offsetX();
        int chosenY = (options.y() == null ? y : options.y()) + options.offsetY();
        // Coordinates use Create's overlay origin; leave its tooltip padding and header clearance.
        int maximumX = Math.max(4, screenWidth - (int)Math.ceil((width + 20) * options.scale()));
        int minimumY = 4 + (int)Math.ceil(16 * options.scale());
        int maximumY = Math.max(minimumY, screenHeight - (int)Math.ceil((height + 8) * options.scale()));
        return new int[]{Math.max(4, Math.min(chosenX, maximumX)), Math.max(minimumY, Math.min(chosenY, maximumY))};
    }
    public static void scale(PoseStack pose, List<?> lines, int x, int y) {
        GogglesOverlayOptions options = GogglesTooltipDispatcher.options(lines);
        if (options == null || options.scale() == 1) return;
        pose.translate(x, y, 0);
        pose.scale((float)options.scale(), (float)options.scale(), 1);
        pose.translate(-x, -y, 0);
    }
    public static boolean headerVisible(GogglesOverlayOptions options) {
        if (options == null) return true;
        var first = options.rows().get(0);
        // Create puts its goggles icon in the first row's icon area. A scripted item/fluid
        // row supplies that icon itself; native text and progress rows retain the goggles.
        return first == null || first.isProgress();
    }
    public static int color(Integer selected, int nativeColor, boolean hidden, float fade) {
        if (hidden) return 0;
        if (selected == null) return nativeColor;
        int alpha = (int)(((selected >>> 24) & 255) * Math.max(0, Math.min(1, fade)));
        return (selected & 0xffffff) | (alpha << 24);
    }
    public static void draw(GuiGraphics graphics, List<? extends FormattedText> lines, int originX, int originY, int screenWidth,
                            int background, int borderTop, int borderBottom, Font font, GogglesOverlayOptions options) {
        var layout = layout(lines, font, options, screenWidth);
        int width = layout.width(), height = layout.height();
        int x = originX + 12, y = originY - 12;
        PoseStack pose = graphics.pose(); pose.pushPose(); pose.translate(0, 0, 400);
        if (!options.noBackground()) {
            graphics.fill(x - 4, y - 4, x + width + 4, y + height + 4, background);
            graphics.fillGradient(x - 3, y - 3, x - 2, y + height + 3, borderTop, borderBottom);
            graphics.fillGradient(x + width + 2, y - 3, x + width + 3, y + height + 3, borderTop, borderBottom);
            graphics.fill(x - 3, y - 3, x + width + 3, y - 2, borderTop);
            graphics.fill(x - 3, y + height + 2, x + width + 3, y + height + 3, borderBottom);
        }
        int lineY = y;
        for (int index = 0; index < lines.size(); index++) {
            var row = options.rows().get(index);
            var rowLayout = layout.rows().get(index);
            int labelX = x;
            if (row != null && !row.isProgress()) {
                var item = row.item();
                var fluid = row.fluid();
                if (!item.isEmpty()) {
                    graphics.renderItem(item, x, lineY); graphics.renderItemDecorations(font, item, x, lineY);
                } else if (!fluid.isEmpty()) {
                    var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
                    var texture = extension.getStillTexture(fluid);
                    if (texture != null) {
                        int tint = extension.getTintColor(fluid);
                        var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
                        graphics.setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f, (tint >>> 24) / 255f);
                        graphics.blit(x, lineY, 0, 16, 16, sprite); graphics.setColor(1, 1, 1, 1);
                    }
                }
                labelX += 20;
            }
            int labelY = lineY + (row != null && !row.isProgress() ? Math.max(0, (16 - rowLayout.text().size() * font.lineHeight) / 2) : 0);
            for (var text : rowLayout.text()) {
                graphics.drawString(font, text, labelX, labelY, 0xffffff, true);
                labelY += font.lineHeight;
            }
            if (row != null && row.isProgress()) {
                int barY = labelY + 3;
                int barWidth = Math.min(row.width(), layout.maximumWidth());
                graphics.fill(x, barY, x + barWidth, barY + 6, 0xff222222);
                graphics.fill(x, barY, x + (int)Math.round(barWidth * row.progress()), barY + 6, row.color());
            }
            lineY += rowLayout.height() + options.lineSpacing() + (index == 0 && lines.size() > 1 ? 2 : 0);
        }
        pose.popPose();
    }
    private static Layout layout(List<?> lines, Font font, GogglesOverlayOptions options, int screenWidth) {
        // The immutable snapshot belongs to one client tick. Reuse measurements for the header,
        // the actual tooltip and subsequent rendered frames instead of wrapping every frame.
        if (cachedOptions == options && cachedFont == font && cachedScreenWidth == screenWidth) return cachedLayout;
        int maximumWidth = Math.max(1, (int)Math.floor((screenWidth - 28) / options.scale()));
        int width = 0, height = 0;
        var rows = new ArrayList<RowLayout>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            var row = options.rows().get(i);
            int prefix = row != null && !row.isProgress() ? 20 : 0;
            List<FormattedCharSequence> text = font.split((FormattedText)lines.get(i), Math.max(1, maximumWidth - prefix));
            int textWidth = text.stream().mapToInt(font::width).max().orElse(0) + prefix;
            int rowHeight = Math.max(font.lineHeight, text.size() * font.lineHeight);
            if (row != null) rowHeight = row.isProgress() ? rowHeight + 10 : Math.max(16, rowHeight);
            width = Math.max(width, row != null && row.isProgress() ? Math.max(textWidth, Math.min(row.width(), maximumWidth)) : textWidth);
            height += rowHeight + (i == 0 ? 0 : options.lineSpacing());
            rows.add(new RowLayout(text, rowHeight));
        }
        var result = new Layout(rows, width, height + (lines.size() > 1 ? 2 : 0), maximumWidth);
        cachedOptions = options; cachedFont = font; cachedScreenWidth = screenWidth; cachedLayout = result;
        return result;
    }
    private record RowLayout(List<FormattedCharSequence> text, int height) { }
    private record Layout(List<RowLayout> rows, int width, int height, int maximumWidth) { }
    private GogglesOverlayRenderer() { }
}
