package net.quiwh.cbjs.content;

import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.latvian.mods.kubejs.core.ItemKJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Native wrench interactions; JSON owns its appearance, including the default composite gear. */
public final class ScriptedWrenchItem extends WrenchItem implements ItemKJS {
    private final ScriptedEquipmentItemData data;
    public ScriptedWrenchItem(ItemBuilder builder) {
        super(builder.createItemProperties()); data = new ScriptedEquipmentItemData(builder);
    }
    @Override public Component getName(ItemStack stack) { return data.name(kjs$getItemBehavior(), () -> super.getName(stack)); }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) {
        return data.builder.burnTime > 0 ? data.builder.burnTime : super.getBurnTime(stack, type);
    }
    @Override @OnlyIn(Dist.CLIENT) public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Create's renderer injects a fixed-material rotating gear even for unrelated custom models.
    }
}
