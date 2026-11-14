package net.quiwh.cbjs.mixin;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Uses Create's real item implementation for both blaze capture and normal block placement. */
@Mixin(value = BlazeBurnerBlockItem.class, remap = false)
public interface BlazeBurnerItemFactory {
    @Invoker("<init>")
    static BlazeBurnerBlockItem cbjs$create(Block block, Item.Properties properties, boolean capturedBlaze) {
        throw new AssertionError("Blaze Burner constructor mixin was not applied");
    }
}
