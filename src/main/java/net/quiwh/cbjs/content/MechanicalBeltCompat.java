package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Shared boundaries for Create's native belts. Material belongs to the registered block. */
public final class MechanicalBeltCompat {
    private static final ThreadLocal<BlockState> PLACEMENT = new ThreadLocal<>();

    /** Bare custom shafts are valid pulleys; encased shafts and other kinetics are not. */
    public static boolean isShaft(BlockState state) {
        return state.getBlock() instanceof ScriptedShaftBlock || ShaftBlock.isShaft(state);
    }

    public static boolean isBelt(BlockState state) {
        return state.getBlock() instanceof BeltBlock;
    }

    public static boolean recognizeBelt(BlockEntry<?> entry, BlockState state) {
        return entry == AllBlocks.BELT ? isBelt(state) : entry.has(state);
    }

    public static boolean sameFamily(BlockState first, BlockState second) {
        return first.getBlock() == second.getBlock();
    }

    public static ItemStack connector(BlockState state) {
        return state.getBlock() instanceof ScriptedMechanicalBeltBlock ? new ItemStack(state.getBlock().asItem())
                : AllItems.BELT_CONNECTOR.asStack();
    }

    public static boolean matchesConnector(BlockState state, ItemStack item) {
        if (state.getBlock() instanceof ScriptedMechanicalBeltBlock)
            return item.getItem() instanceof ScriptedBeltConnectorItem connector
                    && connector.getBlock() == state.getBlock();
        return AllItems.BELT_CONNECTOR.isIn(item);
    }

    public static boolean recognizeConnector(ItemEntry<?> entry, ItemStack item, BlockState state) {
        return entry == AllItems.BELT_CONNECTOR ? matchesConnector(state, item) : entry.isIn(item);
    }

    public static BlockState placementState(BlockState fallback) {
        BlockState selected = PLACEMENT.get();
        return selected == null ? fallback : selected;
    }

    public static void withPlacementState(BlockState state, Runnable placement) {
        BlockState previous = PLACEMENT.get();
        PLACEMENT.set(state);
        try { placement.run(); }
        finally {
            if (previous == null) PLACEMENT.remove();
            else PLACEMENT.set(previous);
        }
    }

    public static void createBelts(Level level, BlockPos start, BlockPos end, BlockState state) {
        withPlacementState(state.getBlock().defaultBlockState(), () -> BeltConnectorItem.createBelts(level, start, end));
    }

    private MechanicalBeltCompat() { }
}
