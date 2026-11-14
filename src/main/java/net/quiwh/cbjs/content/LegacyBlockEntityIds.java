package net.quiwh.cbjs.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.CBJS;

/** Keeps saved block entity data readable after the mod ID changed to cbjs. */
public final class LegacyBlockEntityIds {
    private LegacyBlockEntityIds() {
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        var registry = event.getRegistry();

        // Block entity registries are not saved by Forge, so MissingMappingsEvent cannot migrate these IDs.
        // Install aliases during registration so all NBT readers can resolve existing saved data.
        for (ResourceLocation current : java.util.Set.copyOf(registry.keySet())) {
            if (!current.getNamespace().equals(CBJS.MOD_ID)) continue;
            for (String namespace : new String[]{"kineticjs", "cwi_core"}) {
                ResourceLocation previous = ResourceLocation.fromNamespaceAndPath(namespace, current.getPath());
                if (!registry.containsKey(previous)) registry.addAlias(previous, current);
            }
        }
    }
}
