package net.quiwh.cbjs.content;

import dev.latvian.mods.kubejs.item.ItemBehavior;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/** Item components, including attributes, are applied by createItemProperties(). */
final class ScriptedEquipmentItemData {
    final ItemBuilder builder;
    ScriptedEquipmentItemData(ItemBuilder builder) {
        this.builder = builder;
    }
    Component name(ItemBehavior behavior, Supplier<Component> fallback) {
        return behavior != null && behavior.displayName != null && behavior.formattedDisplayName
                ? behavior.displayName : fallback.get();
    }
}
