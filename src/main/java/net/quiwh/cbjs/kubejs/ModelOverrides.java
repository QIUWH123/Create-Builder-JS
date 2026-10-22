package net.quiwh.cbjs.kubejs;

import com.google.gson.JsonObject;
import dev.latvian.mods.kubejs.block.BlockBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Script-selected resource parents; Minecraft resolves models when resources are loaded. */
public final class ModelOverrides {
    private final BlockBuilder owner;
    private final Set<String> primaryRoles;
    private final Set<String> partRoles;
    private final Map<String, String> parts = new LinkedHashMap<>();
    private boolean primaryItem;

    public ModelOverrides(BlockBuilder owner, Set<String> primaryRoles, Set<String> partRoles) {
        this.owner = java.util.Objects.requireNonNull(owner);
        this.primaryRoles = Set.copyOf(primaryRoles);
        this.partRoles = Set.copyOf(partRoles);
    }

    /** Named body selection is independent of the item; the one-argument API keeps KubeJS semantics. */
    public void set(String role, String path) {
        if (!primaryRoles.contains(role) && !partRoles.contains(role) && !"item".equals(role)) {
            Set<String> supported = new TreeSet<>(primaryRoles);
            supported.addAll(partRoles);
            supported.add("item");
            throw new IllegalArgumentException("Unknown model role '" + role + "' for " + owner.id
                    + "; supported roles: " + String.join(", ", supported));
        }
        String model = validPath(path);
        if ("item".equals(role)) {
            if (owner.itemBuilder == null)
                throw new IllegalArgumentException("Cannot assign an item model to itemless block " + owner.id);
            owner.itemBuilder.parentModel(model);
            primaryItem = false;
        } else if (primaryRoles.contains(role)) owner.model = model;
        else parts.put(role, model);
    }

    public static String validPath(String path) {
        if (path == null || path.isBlank())
            throw new IllegalArgumentException("Model path must not be empty");
        return ResourceLocation.parse(path).toString();
    }

    public boolean has(String role) { return parts.containsKey(role); }
    public String get(String role) { return parts.get(role); }
    /** The single-path API selects the same appearance for both block and item. */
    public void primarySelected() { primaryItem = true; }
    public boolean usesPrimaryItem() {
        return primaryItem && owner.itemBuilder != null && owner.model != null && !owner.model.isEmpty()
                && owner.model.equals(owner.itemBuilder.parentModel);
    }
    public boolean customPrimary() {
        return owner.modelJson != null || owner.model != null && !owner.model.isEmpty();
    }

    public JsonObject primaryModel(String nativeParent) {
        if (owner.modelJson != null) return owner.modelJson.deepCopy();
        return GearAssets.parent(owner.model != null && !owner.model.isEmpty() ? owner.model : nativeParent);
    }

    public JsonObject partModel(String role, String nativeParent) {
        return GearAssets.parent(parts.getOrDefault(role, nativeParent));
    }
}
