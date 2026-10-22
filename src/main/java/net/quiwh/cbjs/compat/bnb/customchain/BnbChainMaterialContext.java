package net.quiwh.cbjs.compat.bnb.customchain;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.quiwh.cbjs.content.ChainMaterials;

/** Short scopes around native placement, refunds and rendering; no per-tick work. */
public final class BnbChainMaterialContext {
    private static final ThreadLocal<ResourceLocation> PLACING = new ThreadLocal<>();
    private static final ThreadLocal<ResourceLocation> REFUNDING = new ThreadLocal<>();
    private static final ThreadLocal<RenderMaterial> RENDERING = new ThreadLocal<>();

    public static ResourceLocation placing() { return PLACING.get(); }
    public static ResourceLocation refunding() { return REFUNDING.get(); }
    public static ResourceLocation rendering() { return RENDERING.get() == null ? null : RENDERING.get().id(); }
    public static ResourceLocation renderingTexture() { return RENDERING.get() == null ? null : RENDERING.get().texture(); }
    public static Scope placing(ResourceLocation id) { return new Scope(PLACING, id); }
    public static Scope refunding(ResourceLocation id) { return new Scope(REFUNDING, id); }
    public static RenderScope rendering(ResourceLocation id) { return new RenderScope(id); }

    public static ResourceLocation material(CogwheelChainBlockEntity entity) {
        if (entity.getChain() instanceof BnbChainMaterial material)
            return material.cbjs$chainMaterial();
        if (entity.getControllerOffset() != null && entity.getLevel() != null
                && entity.getLevel().getBlockEntity(entity.getBlockPos().offset(entity.getControllerOffset()))
                instanceof CogwheelChainBlockEntity controller
                && controller.getChain() instanceof BnbChainMaterial material)
            return material.cbjs$chainMaterial();
        return ChainMaterials.VANILLA;
    }

    public static final class Scope implements AutoCloseable {
        private final ThreadLocal<ResourceLocation> slot;
        private final ResourceLocation previous;
        private Scope(ThreadLocal<ResourceLocation> slot, ResourceLocation current) {
            this.slot = slot;
            previous = slot.get();
            if (current == null) slot.remove(); else slot.set(current);
        }
        @Override public void close() {
            if (previous == null) slot.remove(); else slot.set(previous);
        }
    }
    private record RenderMaterial(ResourceLocation id, ResourceLocation texture) { }
    public static final class RenderScope implements AutoCloseable {
        private final RenderMaterial previous;
        private RenderScope(ResourceLocation id) {
            previous = RENDERING.get();
            RENDERING.set(new RenderMaterial(id, id == null || id.equals(ChainMaterials.VANILLA)
                    ? null : ChainMaterials.texture(id)));
        }
        @Override public void close() {
            if (previous == null) RENDERING.remove(); else RENDERING.set(previous);
        }
    }
    private BnbChainMaterialContext() { }
}
