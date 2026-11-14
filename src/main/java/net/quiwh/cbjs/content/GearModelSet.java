package net.quiwh.cbjs.content;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Resource paths shared by stationary, item, renderer and Flywheel gear models. */
public record GearModelSet(ResourceLocation small, ResourceLocation large, ResourceLocation shaft,
                           ResourceLocation shaftHalf) {
    public static final ResourceLocation COGWHEEL_SHAFT = ResourceLocation.parse("create:block/cogwheel_shaft");
    public static final ResourceLocation SHAFT_HALF = ResourceLocation.parse("create:block/shaft_half");

    public GearModelSet {
        Objects.requireNonNull(small);
        Objects.requireNonNull(large);
        Objects.requireNonNull(shaft);
        Objects.requireNonNull(shaftHalf);
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
