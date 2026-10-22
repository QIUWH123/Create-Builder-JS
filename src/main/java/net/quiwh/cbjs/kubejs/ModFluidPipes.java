package net.quiwh.cbjs.kubejs;

import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.FluidPipeClientModels;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.ModFluidPipeBlockEntities;

import java.util.ArrayList;
import java.util.List;

/** Installs Create hooks once all KubeJS pipe family blocks have been registered. */
public final class ModFluidPipes {
    private static final List<FluidPipeBuilder> BUILDERS = new ArrayList<>();
    private static boolean hooksRegistered;
    private static boolean encasingRegistered;

    public static void add(FluidPipeBuilder builder) {
        BUILDERS.add(builder);
        switch (builder.kind) {
            case REGULAR -> ModFluidPipeBlockEntities.addPipe(builder);
            case GLASS -> ModFluidPipeBlockEntities.addGlassPipe(builder);
            case ENCASED -> CustomCasingBlockEntities.addPipe(builder);
        }
    }

    public static void onRegister(RegisterEvent event) {
        if (hooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            for (var builder : BUILDERS) FluidPipeClientModels.register(builder);
        });
        hooksRegistered = true;
    }

    public static void registerEncasing() {
        if (encasingRegistered) return;
        for (var builder : BUILDERS) {
            if (builder.kind != FluidPipeBuilder.Kind.ENCASED) continue;
            EncasingRegistry.addVariant((FluidPipeBlock) builder.source().get(), (EncasedPipeBlock) builder.get());
        }
        encasingRegistered = true;
    }

    private ModFluidPipes() { }
}
