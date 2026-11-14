package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Preserve the native tunnel placement side effect with custom andesite identities. */
public final class ScriptedTunnelItem extends BeltTunnelItem {
    public ScriptedTunnelItem(Block block, Properties properties) { super(block, properties); }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        // Native BeltTunnelItem classifies every custom block as brass. Correct only a previously bare belt.
        var belt = level.isClientSide ? null : BeltHelper.getSegmentBE(level, pos.below());
        boolean bare = belt != null && belt.casing == CasingType.NONE;
        boolean result = super.updateCustomBlockEntityTag(pos, level, player, stack, state);
        if (bare && state.getBlock() instanceof ScriptedTunnelBlock) belt.setCasingType(CasingType.ANDESITE);
        return result;
    }
}
