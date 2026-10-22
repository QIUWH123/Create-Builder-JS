package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Immutable registration-time OR selectors with live registry tag membership. */
final class RegistrySelector<T> {
    private final List<Entry<T>> entries;
    private final boolean wildcard;

    private RegistrySelector(List<Entry<T>> entries, boolean wildcard) {
        this.entries = entries;
        this.wildcard = wildcard;
    }

    static boolean isList(Object value) {
        Object unwrapped = Wrapper.unwrapped(value);
        return unwrapped instanceof List<?> || unwrapped != null && unwrapped.getClass().isArray();
    }

    static Object scalarValue(Object value) {
        Object unwrapped = Wrapper.unwrapped(value);
        return unwrapped instanceof CharSequence text ? text.toString() : unwrapped;
    }

    static List<?> values(Object value) {
        Object unwrapped = Wrapper.unwrapped(value);
        if (unwrapped instanceof List<?> list) return list;
        if (unwrapped != null && unwrapped.getClass().isArray()) {
            var result = new ArrayList<Object>(Array.getLength(unwrapped));
            for (int i = 0; i < Array.getLength(unwrapped); i++) result.add(Array.get(unwrapped, i));
            return result;
        }
        return java.util.Collections.singletonList(unwrapped);
    }

    static <T> RegistrySelector<T> parse(Object value, ResourceKey<? extends Registry<T>> registry,
                                        String description) {
        boolean list = isList(value);
        var entries = new LinkedHashSet<Entry<T>>();
        boolean wildcard = false;
        for (Object member : values(value)) {
            Object scalar = scalarValue(member);
            if (scalar == null && !list || "*".equals(scalar)) {
                wildcard = true;
                continue;
            }
            boolean tag = scalar instanceof String text && text.startsWith("#");
            ResourceLocation id = scalar instanceof ResourceLocation location ? location
                    : scalar instanceof String text ? ResourceLocation.tryParse(tag ? text.substring(1) : text) : null;
            if (id == null || id.getPath().isEmpty()) {
                throw new IllegalArgumentException("Invalid " + description + " selector '" + scalar
                        + "': expected an ID, #tag, *, null, or a flat list of IDs, #tags and *");
            }
            entries.add(tag ? new Entry<>(null, TagKey.create(registry, id)) : new Entry<>(id, null));
        }
        return new RegistrySelector<>(List.copyOf(entries), wildcard);
    }

    boolean isEmpty() {
        return !wildcard && entries.isEmpty();
    }

    boolean matches(Holder<T> holder) {
        if (wildcard) return true;
        for (Entry<T> entry : entries) {
            if (entry.tag != null ? holder.is(entry.tag) : holder.is(entry.id)) return true;
        }
        return false;
    }

    private record Entry<T>(ResourceLocation id, TagKey<T> tag) {}
}
