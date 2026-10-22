package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.api.equipment.goggles.IHaveHoveringInformation;
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.kubejs.GogglesTooltipDispatcher;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Apply scripts after all native tooltip text and before the final empty guard. */
@Mixin(value = GoggleOverlayRenderer.class, remap = false)
public abstract class GogglesTooltipMixin {
    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/api/equipment/goggles/IHaveHoveringInformation;addToTooltip(Ljava/util/List;Z)Z"))
    private static boolean cbjs$hovering(IHaveHoveringInformation info, List<Component> lines,
                                              boolean shift, Operation<Boolean> original,
                                              @Local(index = 16) boolean gogglesAdded) {
        boolean added = original.call(info, lines, shift);
        // Create 6.0.8 stores its goggles result in local 16. Retain its separator cleanup
        // when goggles already supplied text; mapped and released bytecode are regression checked.
        if (added || gogglesAdded) return added;
        return cbjs$hasMatchingListener();
    }

    @WrapOperation(method = "renderOverlay", at = @At(value = "INVOKE", target =
            "Ljava/util/List;isEmpty()Z", ordinal = 2))
    private static boolean cbjs$finished(List<Component> lines, Operation<Boolean> original,
                                              @Local(index = 13) boolean shift) {
        cbjs$scriptTooltip(lines, shift);
        return original.call(lines);
    }

    @Unique
    private static boolean cbjs$hasMatchingListener() {
        if (!CBJSEvents.GOGGLES_TOOLTIP.hasListeners()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !GogglesItem.isWearingGoggles(mc.player)
                || !(mc.hitResult instanceof BlockHitResult hit)) return false;
        var pos = GoggleOverlayRenderer.proxiedOverlayPosition(mc.level, hit.getBlockPos());
        var id = ForgeRegistries.BLOCKS.getKey(mc.level.getBlockState(pos).getBlock());
        return CBJSEvents.GOGGLES_TOOLTIP.hasListeners(id);
    }

    @Unique
    private static boolean cbjs$scriptTooltip(List<Component> lines, boolean shift) {
        if (!CBJSEvents.GOGGLES_TOOLTIP.hasListeners()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || !GogglesItem.isWearingGoggles(mc.player)
                || !(mc.hitResult instanceof BlockHitResult hit)) return false;
        var pos = GoggleOverlayRenderer.proxiedOverlayPosition(mc.level, hit.getBlockPos());
        return GogglesTooltipDispatcher.apply(mc.level, pos, mc.player, hit.getDirection(), shift,
                mc.level.getGameTime(), lines);
    }
}
