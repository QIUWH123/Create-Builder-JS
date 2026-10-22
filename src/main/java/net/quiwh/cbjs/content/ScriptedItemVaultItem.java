package net.quiwh.cbjs.content;

import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Extends one matching-material end slice; sneak placement always places a single block. */
public final class ScriptedItemVaultItem extends BlockItem {
    public ScriptedItemVaultItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult place(BlockPlaceContext context) {
        var result = super.place(context);
        if (result.consumesAction()) placeSlice(context);
        return result;
    }
    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null) for (String key : new String[]{"Size", "Length", "Controller", "LastKnownPos"}) tag.remove(key);
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
    private void placeSlice(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown() || SymmetryWandItem.presentInHotbar(player)) return;
        var level = context.getLevel();
        Direction face = context.getClickedFace();
        BlockPos on = context.getClickedPos().relative(face.getOpposite());
        if (!level.getBlockState(on).is(getBlock()) || !(level.getBlockEntity(on) instanceof ScriptedItemVaultBlockEntity part)) return;
        var root = part.getControllerBE();
        if (root == null || root.getWidth() == 1 || face.getAxis() != root.getMainConnectionAxis()) return;
        Direction forward = Direction.fromAxisAndDirection(root.getMainConnectionAxis(), Direction.AxisDirection.POSITIVE);
        BlockPos start = face.getAxisDirection() == Direction.AxisDirection.NEGATIVE
                ? root.getBlockPos().relative(forward.getOpposite()) : root.getBlockPos().relative(forward, root.getHeight());
        if (coordinate(start, face.getAxis()) != coordinate(context.getClickedPos(), face.getAxis())) return;
        java.util.List<BlockPos> positions = new java.util.ArrayList<>();
        for (int a = 0; a < root.getWidth(); a++) for (int b = 0; b < root.getWidth(); b++) {
            BlockPos pos = switch (face.getAxis()) {
                case X -> start.offset(0, a, b); case Y -> start.offset(a, 0, b); case Z -> start.offset(a, b, 0);
            };
            var state = level.getBlockState(pos);
            if (state.is(getBlock())) continue;
            if (!state.canBeReplaced()) return;
            positions.add(pos);
        }
        if (!player.isCreative() && context.getItemInHand().getCount() < positions.size()) return;
        try {
            player.getPersistentData().putBoolean("SilenceVaultSound", true);
            for (BlockPos pos : positions) super.place(BlockPlaceContext.at(context, pos, face));
        } finally { player.getPersistentData().remove("SilenceVaultSound"); }
    }
    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return axis.choose(pos.getX(), pos.getY(), pos.getZ());
    }
}
