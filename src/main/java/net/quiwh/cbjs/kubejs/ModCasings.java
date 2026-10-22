package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.fluids.PipeAttachmentModel;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.FluidPipeClientModels;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Defers Create hooks until KubeJS has registered blocks from every namespace. */
public final class ModCasings {
    private static final List<CasingVariantBuilder> VARIANTS = new ArrayList<>();
    private static final List<CasingVariantBuilder> PIPE_VARIANTS = new ArrayList<>();
    private static final List<CasingVariantBuilder> PIPE_VARIANT_VIEW = Collections.unmodifiableList(PIPE_VARIANTS);
    private static final List<CasingBuilder> CASINGS = new ArrayList<>();
    private static boolean hooksRegistered;
    private static boolean encasingRegistered;

    static void add(CasingVariantBuilder variant) {
        VARIANTS.add(variant);
        if (variant.kind == CasingVariantBuilder.Kind.PIPE) PIPE_VARIANTS.add(variant);
    }
    static void addCasing(CasingBuilder casing) { CASINGS.add(casing); }

    static List<CasingVariantBuilder> pipeVariants() {
        return PIPE_VARIANT_VIEW;
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE) || hooksRegistered) return;
        for (var variant : VARIANTS)
            if (variant.kind != CasingVariantBuilder.Kind.PIPE)
                BlockStressValues.IMPACTS.register(variant.get(), () -> 0);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ModCasings::registerClient);
        hooksRegistered = true;
    }

    @SuppressWarnings("removal")
    private static void registerClient() {
        CasingClientTextures.register(CASINGS, VARIANTS);
        for (var variant : VARIANTS) {
            if (variant.kind == CasingVariantBuilder.Kind.PIPE) {
                if (variant.pipe == null)
                    CreateRegistrate.blockModel(() -> PipeAttachmentModel::withAO).accept(variant.get());
                else FluidPipeClientModels.register(variant.get(), variant.pipe.source());
            }
            else ItemBlockRenderTypes.setRenderLayer(variant.get(), RenderType.cutoutMipped());
        }
    }

    public static void registerEncasing() {
        if (encasingRegistered) return;
        for (var variant : VARIANTS) EncasingRegistry.addVariant((Block & EncasableBlock) variant.originalBlock(),
                (Block & EncasedBlock) variant.get());
        encasingRegistered = true;
    }

    private ModCasings() { }
}
