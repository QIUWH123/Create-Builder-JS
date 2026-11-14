package net.quiwh.cbjs.mixin;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorHandler;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import net.quiwh.cbjs.content.ScriptedBeltConnectorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.Group;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.MechanicalBeltLengths;

@Mixin(value = BeltConnectorHandler.class, remap = false)
public abstract class MechanicalBeltConnectorHandlerMixin {
    @Group(name = "cbjs$previewDistance", min = 1, max = 1)
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$previewDistance(BlockPos second, Vec3i first, double nativeCap,
            Operation<Boolean> original, @Local ItemStack heldItem) {
        Integer cap = MechanicalBeltLengths.customConfigured(MechanicalBeltLengths.material(heldItem.getItem()));
        return cap == null ? original.call(second, first, nativeCap) : MechanicalBeltLengths.within(first, second, cap);
    }

    @Group(name = "cbjs$previewDistance", min = 1, max = 1)
    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;m_123314_(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$previewDistanceSrg(BlockPos second, Vec3i first, double nativeCap,
            Operation<Boolean> original, @Local ItemStack heldItem) {
        return cbjs$previewDistance(second, first, nativeCap, original, heldItem);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;canConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"),
            remap = false, require = 1, allow = 1)
    private static boolean cbjs$previewLength(Level level, BlockPos first, BlockPos second,
            Operation<Boolean> original, @Local ItemStack heldItem) {
        return MechanicalBeltLengths.withBelt(MechanicalBeltLengths.material(heldItem.getItem()),
                () -> original.call(level, first, second));
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ShaftBlock;isShaft(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            remap = false, require = 1, allow = 1)
    private static boolean cbjs$previewCustomShaft(BlockState state) {
        return MechanicalBeltCompat.isShaft(state);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false, require = 1, allow = 1)
    private static boolean cbjs$previewCustomConnector(ItemEntry<?> entry, ItemStack item) {
        return entry == AllItems.BELT_CONNECTOR && item.getItem() instanceof ScriptedBeltConnectorItem || entry.isIn(item);
    }
}
