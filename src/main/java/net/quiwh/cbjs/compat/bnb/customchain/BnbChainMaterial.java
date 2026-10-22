package net.quiwh.cbjs.compat.bnb.customchain;

import net.minecraft.resources.ResourceLocation;

/** Material stored on native BnB loops, including loops using native cogwheels. */
public interface BnbChainMaterial {
    ResourceLocation cbjs$chainMaterial();
    void cbjs$chainMaterial(ResourceLocation material);
}
