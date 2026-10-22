package net.quiwh.cbjs.kubejs;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** One committed processing result; heldItem is the unprocessed remainder, not an output. */
public final class BeltItemProcessedEventJS extends BeltItemEventJS {
    private final List<ItemStack> outputs;
    private final ItemStack heldItem;
    private final BlockPos processorPos;
    private final String processorId;
    public BeltItemProcessedEventJS(Level level, BlockPos pos, BlockPos controller, Block block, ItemStack input,
                                   List<ItemStack> outputs, ItemStack heldItem,
                                   @Nullable BlockPos processorPos, @Nullable String processorId) {
        super(level, pos, controller, block, input);
        this.outputs = outputs.stream().map(ItemStack::copy).toList();
        this.heldItem = heldItem.copy();
        this.processorPos = processorPos == null ? null : processorPos.immutable();
        this.processorId = processorId;
    }
    public List<ItemStack> getOutputs() { return outputs.stream().map(ItemStack::copy).toList(); }
    public ItemStack getHeldItem() { return heldItem.copy(); }
    @Nullable public BlockPos getProcessorPos() { return processorPos; }
    @Nullable public String getProcessorId() { return processorId; }
    @Override boolean matchesItem(RegistrySelector<Item> selector) {
        if (super.matchesItem(selector)) return true;
        for (ItemStack output : outputs) {
            if (!output.isEmpty() && selector.matches(output.getItem().builtInRegistryHolder())) return true;
        }
        return false;
    }
}
