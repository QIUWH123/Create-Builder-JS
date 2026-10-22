package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Keeps Create's two-pulley interaction, NBT selection, cooldown and consumption rules. */
public final class ScriptedBeltConnectorItem extends BeltConnectorItem {
    private final Block belt;
    public ScriptedBeltConnectorItem(Block belt, Item.Properties properties) {
        super(properties);
        this.belt = belt;
    }
    @Override public Block getBlock() { return belt; }
}
