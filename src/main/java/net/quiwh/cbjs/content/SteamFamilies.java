package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Identity extensions are called only at audited native steam call sites. */
public final class SteamFamilies {
    public static boolean has(BlockEntry<?> entry, BlockState state) {
        if (entry.has(state)) return true;
        if (entry == AllBlocks.STEAM_ENGINE) return state.getBlock() instanceof ScriptedSteamEngineBlock;
        if (entry == AllBlocks.STEAM_WHISTLE) return state.getBlock() instanceof ScriptedSteamWhistleBlock;
        if (entry == AllBlocks.STEAM_WHISTLE_EXTENSION) return state.getBlock() instanceof ScriptedSteamWhistleExtensionBlock;
        if (entry == AllBlocks.SHAFT) return state.getBlock() instanceof ScriptedShaftBlock;
        if (entry == AllBlocks.POWERED_SHAFT) return state.getBlock() instanceof ScriptedPoweredShaftBlock;
        return false;
    }
    public static boolean isIn(BlockEntry<?> entry, ItemStack stack) {
        return entry.isIn(stack) || entry == AllBlocks.STEAM_WHISTLE && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof ScriptedSteamWhistleBlock;
    }
    private SteamFamilies() { }
}
