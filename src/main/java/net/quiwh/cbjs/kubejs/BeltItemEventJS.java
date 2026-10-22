package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.level.BlockContainerJS;
import dev.latvian.mods.kubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

/** Completed belt action; item getters return detached copies. */
public abstract class BeltItemEventJS extends LevelEventJS {
    private final Level level;
    private final BlockPos pos;
    private final BlockPos controllerPos;
    private final Block block;
    private final ItemStack item;
    private final String blockId;
    protected BeltItemEventJS(Level level, BlockPos pos, BlockPos controllerPos, Block block, ItemStack item) {
        this.level = level;
        this.pos = pos.immutable();
        this.controllerPos = controllerPos.immutable();
        this.block = block;
        this.item = item.copy();
        this.blockId = ForgeRegistries.BLOCKS.getKey(block).toString();
    }
    @Override public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public BlockPos getControllerPos() { return controllerPos; }
    public BlockContainerJS getBlock() { return new BlockContainerJS(level, pos); }
    public String getBlockId() { return blockId; }
    public ItemStack getItem() { return item.copy(); }
    public String getItemId() { return ForgeRegistries.ITEMS.getKey(item.getItem()).toString(); }
    public int getCount() { return item.getCount(); }
    Block filterBlock() { return block; }
    boolean matchesItem(RegistrySelector<net.minecraft.world.item.Item> selector) {
        return !item.isEmpty() && selector.matches(item.getItem().builtInRegistryHolder());
    }
}
