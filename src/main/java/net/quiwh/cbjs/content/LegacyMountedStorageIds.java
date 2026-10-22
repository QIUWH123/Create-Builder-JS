package net.quiwh.cbjs.content;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Migrates only this addon's dispatch IDs before Create decodes saved contraption storage. */
public final class LegacyMountedStorageIds {
    public static CompoundTag upgrade(CompoundTag source) {
        CompoundTag result = source;
        for (int kind = 0; kind < 2; kind++) {
            String listKey = kind == 0 ? "items" : "fluids";
            var entries = source.getList(listKey, Tag.TAG_COMPOUND);
            for (int index = 0; index < entries.size(); index++) {
                String type = entries.getCompound(index).getCompound("storage").getString("type");
                String replacement = kind == 0 ? switch (type) {
                    case "kineticjs:scripted_item_storage", "cwi_core:scripted_item_storage" -> "cbjs:scripted_item_storage";
                    default -> null;
                } : switch (type) {
                    case "kineticjs:scripted_fluid_tank", "cwi_core:scripted_fluid_tank" -> "cbjs:scripted_fluid_tank";
                    default -> null;
                };
                if (replacement == null) continue;
                if (result == source) result = source.copy();
                result.getList(listKey, Tag.TAG_COMPOUND).getCompound(index)
                        .getCompound("storage").putString("type", replacement);
            }
        }
        return result;
    }

    private LegacyMountedStorageIds() { }
}
