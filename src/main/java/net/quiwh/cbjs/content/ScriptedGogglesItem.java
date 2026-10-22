package net.quiwh.cbjs.content;

import com.google.common.collect.Multimap;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.latvian.mods.kubejs.core.ItemKJS;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/** Keeps Create's equipping and dispenser rules while exposing normal KubeJS item behavior. */
public final class ScriptedGogglesItem extends GogglesItem implements ItemKJS {
    private final ScriptedEquipmentItemData data;
    public ScriptedGogglesItem(ItemBuilder builder) {
        super(builder.createItemProperties());
        data = new ScriptedEquipmentItemData(builder);
        EquipmentItemCompat.registerGogglesPredicate();
    }
    @Override public ItemBuilder kjs$getItemBuilder() { return data.builder; }
    @Override public Component getName(ItemStack stack) { return data.name(() -> super.getName(stack)); }
    @Override public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? data.attributes() : super.getDefaultAttributeModifiers(slot);
    }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) {
        return data.builder.burnTime > 0 ? data.builder.burnTime : super.getBurnTime(stack, type);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // GogglesItem overrides Item.use, so KubeJS's injection into Item.use cannot see it.
        if (data.builder.use != null)
            return data.builder.use.use(level, player, hand) ? ItemUtils.startUsingInstantly(level, player, hand)
                    : InteractionResultHolder.fail(player.getItemInHand(hand));
        return super.use(level, player, hand);
    }
}
