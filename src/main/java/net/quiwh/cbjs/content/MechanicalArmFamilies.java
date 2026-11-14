package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

/** Restricts native identity compatibility to Create's mechanical-arm registry entry. */
public final class MechanicalArmFamilies {
    public static boolean isArmItem(BlockEntry<?> entry, ItemStack stack) {
        return entry == AllBlocks.MECHANICAL_ARM && stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof ScriptedMechanicalArmBlock;
    }
    private MechanicalArmFamilies() { }
}
