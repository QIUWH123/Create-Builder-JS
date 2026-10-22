package net.quiwh.cbjs.content;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.latvian.mods.kubejs.item.ItemBuilder;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.function.Supplier;

/** The basic KubeJS item behavior not supplied by its universal Item mixin. */
final class ScriptedEquipmentItemData {
    final ItemBuilder builder;
    private Multimap<Attribute, AttributeModifier> attributes;
    ScriptedEquipmentItemData(ItemBuilder builder) { this.builder = builder; }
    Component name(Supplier<Component> fallback) {
        return builder.displayName != null && builder.formattedDisplayName ? builder.displayName : fallback.get();
    }
    Multimap<Attribute, AttributeModifier> attributes() {
        if (attributes == null) {
            attributes = ArrayListMultimap.create();
            builder.attributes.forEach((id, modifier) -> attributes.put(RegistryInfo.ATTRIBUTE.getValue(id), modifier));
        }
        return attributes;
    }
}
