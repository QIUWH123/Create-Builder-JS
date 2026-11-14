package net.quiwh.cbjs.content;

import com.simibubi.create.content.equipment.goggles.GogglesItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Create usage recognition; exact item equality, recipes and other entries stay independent. */
public final class EquipmentItemCompat {
    private static boolean gogglesPredicateRegistered;
    public static boolean matchesCreateEntry(ResourceLocation entry, ItemStack stack) {
        if (stack.isEmpty() || !entry.getNamespace().equals("create")) return false;
        return switch (entry.getPath()) {
            case "wrench" -> stack.getItem() instanceof ScriptedWrenchItem;
            case "goggles" -> stack.getItem() instanceof ScriptedGogglesItem;
            default -> false;
        };
    }
    static synchronized void registerGogglesPredicate() {
        if (gogglesPredicateRegistered) return;
        GogglesItem.addIsWearingPredicate(player -> player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ScriptedGogglesItem);
        gogglesPredicateRegistered = true;
    }
    private EquipmentItemCompat() { }
}
