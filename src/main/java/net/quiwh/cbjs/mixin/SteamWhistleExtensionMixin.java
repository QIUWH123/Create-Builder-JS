package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.SteamFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.content.decoration.steamWhistle.WhistleExtenderBlock;
import net.minecraft.world.item.ItemStack;

@Mixin(value = WhistleExtenderBlock.class, remap = false)
public abstract class SteamWhistleExtensionMixin {
    @Redirect(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"), require = 1)
    private boolean cbjs$whistleItem(BlockEntry<?> entry, ItemStack stack) { return SteamFamilies.isIn(entry, stack); }
    @Redirect(method = "hidesNeighborFace", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private boolean cbjs$whistleFace(BlockEntry<?> entry, BlockState state) { return SteamFamilies.has(entry, state); }
}
