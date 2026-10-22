package net.quiwh.cbjs.content;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.CBJS;

/** Keeps saved block entity data readable after the mod ID changed to cbjs. */
public final class LegacyBlockEntityIds {
    private LegacyBlockEntityIds() {
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCK_ENTITY_TYPES)) return;
        if (!(event.getForgeRegistry() instanceof ForgeRegistry<?> registry)) return;

        // Block entity registries are not saved by Forge, so MissingMappingsEvent cannot migrate these IDs.
        // Install aliases during registration so all NBT readers can resolve existing saved data.
        for (ResourceLocation current : registry.getKeys()) {
            if (!current.getNamespace().equals(CBJS.MOD_ID)) continue;
            for (String namespace : new String[]{"kineticjs", "cwi_core"}) {
                ResourceLocation previous = ResourceLocation.fromNamespaceAndPath(namespace, current.getPath());
                if (!registry.containsKey(previous)) registry.addAlias(previous, current);
            }
        }
    }
}
