package net.quiwh.cbjs.content;

import com.google.common.collect.Multimap;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import dev.latvian.mods.kubejs.core.ItemKJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.api.distmarker.*;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.function.Consumer;

/** Native wrench interactions; JSON owns its appearance, including the default composite gear. */
public final class ScriptedWrenchItem extends WrenchItem implements ItemKJS {
    private final ScriptedEquipmentItemData data;
    public ScriptedWrenchItem(ItemBuilder builder) {
        super(builder.createItemProperties()); data = new ScriptedEquipmentItemData(builder);
    }
    @Override public ItemBuilder kjs$getItemBuilder() { return data.builder; }
    @Override public Component getName(ItemStack stack) { return data.name(() -> super.getName(stack)); }
    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? data.attributes() : super.getDefaultAttributeModifiers(slot);
    }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) {
        return data.builder.burnTime > 0 ? data.builder.burnTime : super.getBurnTime(stack, type);
    }
    @Override @OnlyIn(Dist.CLIENT) public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        // Create's renderer injects a fixed-material rotating gear even for unrelated custom models.
    }
}
