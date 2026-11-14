package net.quiwh.cbjs.content;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.kubejs.BlazeBurnerBuilder;

/** Material selection at native identity boundaries; native acceptance and world updates stay native. */
public final class BlazeBurnerFamilies {
    public static BlazeBurnerBuilder builder(BlockState state) {
        if (state.getBlock() instanceof ScriptedBlazeBurnerBlock block) return block.sourceBuilder();
        if (state.getBlock() instanceof ScriptedLitBlazeBurnerBlock block) return block.sourceBuilder();
        return null;
    }
    public static ItemStack stack(BlockState state) {
        var builder = builder(state);
        if (builder == null) return ItemStack.EMPTY;
        return new ItemStack(state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)
                && state.getValue(BlazeBurnerBlock.HEAT_LEVEL) != BlazeBurnerBlock.HeatLevel.NONE
                ? builder.get().asItem() : builder.emptyItem());
    }
    private BlazeBurnerFamilies() { }
}
