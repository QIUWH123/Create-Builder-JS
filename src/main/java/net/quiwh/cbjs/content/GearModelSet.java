package net.quiwh.cbjs.content;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Resource paths shared by stationary, item, renderer and Flywheel gear models. */
public record GearModelSet(ResourceLocation small, ResourceLocation large, ResourceLocation shaft,
                           ResourceLocation shaftHalf, ResourceLocation complete) {
    public static final ResourceLocation COGWHEEL_SHAFT = ResourceLocation.parse("create:block/cogwheel_shaft");
    public static final ResourceLocation SHAFT_HALF = ResourceLocation.parse("create:block/shaft_half");

    public GearModelSet {
        Objects.requireNonNull(small);
        Objects.requireNonNull(large);
        Objects.requireNonNull(shaft);
        Objects.requireNonNull(shaftHalf);
    }

    public GearModelSet(ResourceLocation small, ResourceLocation large, ResourceLocation shaft,
                        ResourceLocation shaftHalf) {
        this(small, large, shaft, shaftHalf, null);
    }

    /** Complete meshes already contain their shaft; casing windows still need separate parts. */
    public boolean usesCompleteModel(boolean encased) {
        return complete != null && !encased;
    }

    public static GearModelSet imported(String material) {
        return generated(ResourceLocation.fromNamespaceAndPath("kubejs", material));
    }

    public static GearModelSet generated(ResourceLocation id) {
        return generated(id, false);
    }

    public static GearModelSet generated(ResourceLocation id, boolean customShaftTextures) {
        String prefix = "block/" + id.getPath() + "/";
        return new GearModelSet(path(id, prefix + "cogwheel_shaftless"),
                path(id, prefix + "large_cogwheel_shaftless"),
                customShaftTextures ? path(id, prefix + "shaft") : COGWHEEL_SHAFT,
                customShaftTextures ? path(id, prefix + "shaft_half") : SHAFT_HALF);
    }

    private static ResourceLocation path(ResourceLocation id, String path) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path);
    }
}
