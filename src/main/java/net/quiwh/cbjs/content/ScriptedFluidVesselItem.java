package net.quiwh.cbjs.content;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.symmetryWand.SymmetryWandItem;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
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
import net.minecraftforge.fluids.FluidStack;

/** Horizontal end placement and item NBT sanitation, using only Create's public API. */
public final class ScriptedFluidVesselItem extends BlockItem {
    public ScriptedFluidVesselItem(Block block, Properties properties) { super(block, properties); }

    @Override public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction()) placeCrossSection(context);
        return result;
    }

    @Override protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player,
            ItemStack stack, BlockState state) {
        CompoundTag tag = stack.getTagElement("BlockEntityTag");
        if (tag != null && level.getServer() != null) {
            for (String key : new String[]{"Luminosity", "Size", "Height", "Controller", "LastKnownPos", "Uninitialized"})
                tag.remove(key);
            if (tag.contains("TankContent")) {
                FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound("TankContent"));
                int capacity = ((ScriptedFluidVesselBlock)getBlock()).sourceBuilder().capacityPerBlock();
                fluid.setAmount(Math.min(capacity, fluid.getAmount()));
                tag.put("TankContent", fluid.writeToNBT(new CompoundTag()));
            }
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }

    private void placeCrossSection(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown() || SymmetryWandItem.presentInHotbar(player)) return;
        Direction face = context.getClickedFace();
        Level level = context.getLevel();
        BlockPos supportPos = context.getClickedPos().relative(face.getOpposite());
        BlockState support = level.getBlockState(supportPos);
        if (!support.is(getBlock()) || face.getAxis() != support.getValue(ScriptedFluidVesselBlock.AXIS)) return;
        var block = (ScriptedFluidVesselBlock)getBlock();
        FluidTankBlockEntity member = ConnectivityHandler.partAt(block.getBlockEntityType(), level, supportPos);
        FluidTankBlockEntity controller = member == null ? null : member.getControllerBE();
        if (controller == null || controller.getWidth() == 1) return;
        Direction.Axis axis = face.getAxis();
        BlockPos start = controller.getBlockPos().relative(face, face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? controller.getHeight() : 1);
        int clicked = axis.choose(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ());
        if (axis.choose(start.getX(), start.getY(), start.getZ()) != clicked) return;
        int needed = 0, width = controller.getWidth();
        for (int a = 0; a < width; a++) for (int b = 0; b < width; b++) {
            BlockState state = level.getBlockState(start.offset(FluidTankGeometry.offset(axis, 0, a, b)));
            if (state.is(getBlock()) && state.getValue(ScriptedFluidVesselBlock.AXIS) == axis) continue;
            if (!state.canBeReplaced()) return;
            needed++;
        }
        if (!player.isCreative() && context.getItemInHand().getCount() < needed) return;
        for (int a = 0; a < width; a++) for (int b = 0; b < width; b++) {
            BlockPos pos = start.offset(FluidTankGeometry.offset(axis, 0, a, b));
            BlockState state = level.getBlockState(pos);
            if (state.is(getBlock()) && state.getValue(ScriptedFluidVesselBlock.AXIS) == axis) continue;
            player.getPersistentData().putBoolean("SilenceTankSound", true);
            try { super.place(BlockPlaceContext.at(context, pos, face)); }
            finally { player.getPersistentData().remove("SilenceTankSound"); }
        }
    }
}
