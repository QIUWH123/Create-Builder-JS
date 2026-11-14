package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ScriptedBeltConnectorItem;

@Mixin(value = BeltSlicer.class, remap = false)
public abstract class MechanicalBeltSlicerClientMixin {
    @Redirect(method = "tickHoveringInformation", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1, allow = 1)
    private static boolean cbjs$hoverCustomBelt(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }
    @Redirect(method = "tickHoveringInformation", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false, require = 4, allow = 4)
    private static boolean cbjs$hoverCustomConnector(ItemEntry<?> entry, ItemStack item) {
        return entry == AllItems.BELT_CONNECTOR && item.getItem() instanceof ScriptedBeltConnectorItem || entry.isIn(item);
    }
}
