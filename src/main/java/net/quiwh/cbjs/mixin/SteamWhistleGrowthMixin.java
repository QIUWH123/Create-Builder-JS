package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.SteamFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.content.ScriptedSteamWhistleBlock;

@Mixin(value = WhistleBlock.class, remap = false)
public abstract class SteamWhistleGrowthMixin {
    @Redirect(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"), require = 1)
    private boolean cbjs$whistleItem(BlockEntry<?> entry, ItemStack stack) { return SteamFamilies.isIn(entry, stack); }
    @Redirect(method = "incrementSize", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private static boolean cbjs$matchingExtension(BlockEntry<?> entry, BlockState state, LevelAccessor level, BlockPos pos) {
        var base = level.getBlockState(pos);
        if (entry == AllBlocks.STEAM_WHISTLE_EXTENSION && base.getBlock() instanceof ScriptedSteamWhistleBlock block)
            return state.is(block.sourceBuilder().extension.get());
        return entry.has(state);
    }
    @Redirect(method = "incrementSize", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"), require = 1)
    private static BlockState cbjs$createMatchingExtension(BlockEntry<?> entry, LevelAccessor level, BlockPos pos) {
        var base = level.getBlockState(pos);
        return entry == AllBlocks.STEAM_WHISTLE_EXTENSION && base.getBlock() instanceof ScriptedSteamWhistleBlock block
                ? block.sourceBuilder().extension.get().defaultBlockState() : entry.getDefaultState();
    }
}
