package net.quiwh.cbjs.drives;

import com.simibubi.create.AllItems;
import dev.latvian.mods.kubejs.core.BlockKJS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.ChainMaterials;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.kubejs.CogwheelBuilder;

/** Standalone drive materials use only mandatory dependencies. */
public enum DriveKind {
    CHAIN, ROPE, BELT;

    public Item defaultItem() {
        return switch (this) {
            case CHAIN -> Items.CHAIN;
            case ROPE -> Items.STRING;
            case BELT -> AllItems.BELT_CONNECTOR.get();
        };
    }

    public ResourceLocation defaultMaterial() { return BuiltInRegistries.ITEM.getKey(defaultItem()); }

    public boolean validMaterial(ResourceLocation id) {
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) return false;
        Item item = BuiltInRegistries.ITEM.get(id);
        return this == CHAIN ? ChainMaterials.isChain(new ItemStack(item)) : item == defaultItem();
    }

    public static DriveKind fromItem(ItemStack stack) {
        if (ChainMaterials.isChain(stack)) return CHAIN;
        if (stack.is(Items.STRING)) return ROPE;
        if (stack.is(AllItems.BELT_CONNECTOR.get())) return BELT;
        return null;
    }

    public boolean accepts(BlockState state) {
        if (!(state.getBlock() instanceof MaterialGear gear) || gear.isEncased()
                || !(state.getBlock() instanceof BlockKJS scripted)
                || !(scripted.kjs$getBlockBuilder() instanceof CogwheelBuilder builder)) return false;
        return switch (this) {
            case CHAIN -> builder.chainEnabled();
            case ROPE -> builder.ropeEnabled();
            case BELT -> builder.beltEnabled();
        };
    }

    public ResourceLocation texture(ResourceLocation material) {
        return this == CHAIN ? ChainMaterials.texture(material)
                : ResourceLocation.fromNamespaceAndPath("cbjs", "textures/block/drive_" + name().toLowerCase(java.util.Locale.ROOT) + ".png");
    }
}
