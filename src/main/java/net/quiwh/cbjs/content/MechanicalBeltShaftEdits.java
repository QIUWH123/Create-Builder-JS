package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.tterrag.registrate.util.entry.BlockEntry;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Preserves pulley materials while Create moves or merges belt endpoints. */
public final class MechanicalBeltShaftEdits {
    private static final ThreadLocal<Edit> CURRENT = new ThreadLocal<>();

    private MechanicalBeltShaftEdits() {}

    public static InteractionResult edit(boolean connector, BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit, Supplier<InteractionResult> original) {
        Edit edit = capture(connector, state, level, pos, hit);
        Edit previous = CURRENT.get();
        if (edit == null) CURRENT.remove();
        else CURRENT.set(edit);
        try {
            InteractionResult result = original.get();
            if (edit != null && result == InteractionResult.SUCCESS) {
                if (connector && MechanicalBeltCompat.isBelt(level.getBlockState(edit.next))
                        && level.getBlockState(edit.next).getValue(BeltBlock.PART) == BeltPart.MIDDLE) {
                    // Merging consumes both pulleys; they must not reappear after splitting later.
                    MechanicalBeltShafts.set(level.getBlockEntity(edit.next), null);
                } else {
                    MechanicalBeltShafts.set(level.getBlockEntity(edit.next), edit.shaft);
                }
                MechanicalBeltShafts.set(level.getBlockEntity(pos), null);
                if (!edit.deferredRefund.isEmpty())
                    player.getInventory().placeItemBackInInventory(edit.deferredRefund);
            }
            return result;
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    private static Edit capture(boolean connector, BlockState state, Level level, BlockPos pos, BlockHitResult hit) {
        if (level.isClientSide || !hoveringEnd(state, hit)) return null;
        BlockPos vector = BlockPos.containing(BeltHelper.getBeltVector(state));
        boolean end = state.getValue(BeltBlock.PART) == BeltPart.END;
        BlockPos next = end == connector ? pos.offset(vector) : pos.subtract(vector);
        BlockState shaft = MechanicalBeltShafts.get(level.getBlockEntity(pos));
        BlockState nextShaft = MechanicalBeltShafts.get(level.getBlockEntity(next));
        return shaft == null && nextShaft == null ? null : new Edit(connector, next, shaft, nextShaft);
    }

    public static ItemStack shorteningRefund(BlockEntry<?> entry) {
        Edit edit = CURRENT.get();
        return entry == AllBlocks.SHAFT && edit != null && !edit.connector
                ? MechanicalBeltShafts.shaftItem(edit.nextShaft) : entry.asStack();
    }

    public static ItemStack mergingRefund(BlockEntry<?> entry, int amount) {
        Edit edit = CURRENT.get();
        if (entry != AllBlocks.SHAFT || edit == null || !edit.connector || amount != 2)
            return entry.asStack(amount);
        ItemStack first = MechanicalBeltShafts.shaftItem(edit.shaft);
        ItemStack second = MechanicalBeltShafts.shaftItem(edit.nextShaft);
        if (ItemStack.isSameItemSameTags(first, second)) {
            first.grow(second.getCount());
        } else {
            // Create's native refund call accepts one stack, so return the second material on success.
            edit.deferredRefund = second;
        }
        return first;
    }

    private static boolean hoveringEnd(BlockState state, BlockHitResult hit) {
        BeltPart part = state.getValue(BeltBlock.PART);
        if (part != BeltPart.START && part != BeltPart.END) return false;
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
        return (offset.dot(BeltHelper.getBeltVector(state)) > 0) == (part == BeltPart.END);
    }

    private static final class Edit {
        final boolean connector;
        final BlockPos next;
        final BlockState shaft;
        final BlockState nextShaft;
        ItemStack deferredRefund = ItemStack.EMPTY;

        Edit(boolean connector, BlockPos next, BlockState shaft, BlockState nextShaft) {
            this.connector = connector;
            this.next = next;
            this.shaft = shaft;
            this.nextShaft = nextShaft;
        }
    }
}
