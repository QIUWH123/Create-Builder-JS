package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.FormattedText;
import net.createmod.catnip.gui.element.RenderElement;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.quiwh.cbjs.client.GogglesOverlayRenderer;
import net.quiwh.cbjs.kubejs.GogglesTooltipDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Presentation changes are applied after Create's native fade and before both rendering branches. */
@Mixin(value = GoggleOverlayRenderer.class, remap = false)
public abstract class GogglesOverlayLayoutMixin {
    @Inject(method = "renderOverlay", at = @At(value = "INVOKE", target =
            "Lnet/createmod/catnip/gui/element/GuiGameElement;of(Lnet/minecraft/world/item/ItemStack;)Lnet/createmod/catnip/gui/element/GuiGameElement$GuiRenderBuilder;"), require = 1, allow = 1)
    private static void cbjs$layout(ForgeGui gui, GuiGraphics graphics, float partialTicks, int width, int height,
            CallbackInfo callback, @Local(index = 19) List<?> lines, @Local(index = 21) PoseStack pose,
            @Local(index = 25) LocalIntRef x, @Local(index = 26) LocalIntRef y) {
        if (GogglesTooltipDispatcher.options(lines) == null) return;
        int[] resolved = GogglesOverlayRenderer.position(lines, x.get(), y.get(), width, height, Minecraft.getInstance().font);
        x.set(resolved[0]); y.set(resolved[1]);
        GogglesOverlayRenderer.scale(pose, lines, x.get(), y.get());
    }
    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target =
            "Lnet/createmod/catnip/gui/element/RenderElement;render(Lnet/minecraft/client/gui/GuiGraphics;)V"), require = 1, allow = 1)
    private static void cbjs$header(RenderElement element, GuiGraphics graphics, Operation<Void> original,
            @Local(index = 19) List<?> lines) {
        if (GogglesOverlayRenderer.headerVisible(GogglesTooltipDispatcher.options(lines))) original.call(element, graphics);
    }
    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/foundation/gui/RemovedGuiUtils;drawHoveringText(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/List;IIIIIIIILnet/minecraft/client/gui/Font;)V"), require = 2, allow = 2)
    private static void cbjs$rows(GuiGraphics graphics, List<? extends FormattedText> lines, int x, int y,
            int screenWidth, int screenHeight, int maxWidth, int background, int borderTop, int borderBottom,
            Font font, Operation<Void> original, @Local(index = 27) float fade) {
        var options = GogglesTooltipDispatcher.options(lines);
        if (options == null) {
            original.call(graphics, lines, x, y, screenWidth, screenHeight, maxWidth, background, borderTop, borderBottom, font);
            return;
        }
        background = GogglesOverlayRenderer.color(options.background(), background, options.noBackground(), fade);
        borderTop = GogglesOverlayRenderer.color(options.borderTop(), borderTop, options.noBackground(), fade);
        borderBottom = GogglesOverlayRenderer.color(options.borderBottom(), borderBottom, options.noBackground(), fade);
        if (options.customLayout()) GogglesOverlayRenderer.draw(graphics, lines, x, y, screenWidth, background, borderTop, borderBottom, font, options);
        else original.call(graphics, lines, x, y, screenWidth, screenHeight, maxWidth, background, borderTop, borderBottom, font);
    }
}
