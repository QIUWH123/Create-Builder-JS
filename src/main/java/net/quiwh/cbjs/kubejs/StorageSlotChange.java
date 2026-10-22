package net.quiwh.cbjs.kubejs;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** One changed local slot. Member position disambiguates slots across a multiblock. */
public final class StorageSlotChange {
    private final BlockPos memberPos;
    private final int slot;
    private final ItemStack previous;
    private final ItemStack current;
    public StorageSlotChange(BlockPos memberPos, int slot, ItemStack previous, ItemStack current) {
        this.memberPos = memberPos.immutable();
        this.slot = slot;
        this.previous = previous.copy();
        this.current = current.copy();
    }
    public BlockPos getMemberPos() { return memberPos; }
    public int getSlot() { return slot; }
    public int getLocalSlot() { return slot; }
    public ItemStack getOldItem() { return previous.copy(); }
    public ItemStack getItem() { return current.copy(); }
    public ItemStack getNewItem() { return getItem(); }
    public int getOldAmount() { return previous.getCount(); }
    public int getAmount() { return current.getCount(); }
    public int getNewAmount() { return getAmount(); }
    public String getOldItemId() { return itemId(previous); }
    public String getItemId() { return itemId(current); }
    public String getNewItemId() { return getItemId(); }
    ItemStack filterOldItem() { return previous; }
    ItemStack filterItem() { return current; }
    private static String itemId(ItemStack item) {
        return item.isEmpty() ? "minecraft:air" : ForgeRegistries.ITEMS.getKey(item.getItem()).toString();
    }
}
