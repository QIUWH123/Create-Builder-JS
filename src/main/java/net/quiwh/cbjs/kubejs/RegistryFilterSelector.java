package net.quiwh.cbjs.kubejs;

import net.minecraft.resources.ResourceLocation;

/** Strict nonempty registry IDs/tags shared by storage whitelists. */
record RegistryFilterSelector(ResourceLocation id, boolean tagged) {
    static RegistryFilterSelector parse(String setting, String selector) {
        boolean tagged = selector != null && selector.startsWith("#");
        String value = selector == null ? "" : tagged ? selector.substring(1) : selector;
        ResourceLocation id = value.isBlank() ? null : ResourceLocation.tryParse(value);
        if (id == null || id.getPath().isEmpty())
            throw new IllegalArgumentException("Invalid " + setting + " selector: " + selector + "; expected an ID or #tag");
        return new RegistryFilterSelector(id, tagged);
    }
}
