package net.quiwh.cbjs.content;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import net.minecraft.world.level.block.Block;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockModel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.ModPartialModels;
import net.quiwh.cbjs.client.ModSpriteShifts;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Script registration metadata. Suppliers are never resolved during startup builder creation. */
public final class ModCogwheels {
    private record Encased(Supplier<? extends MaterialCogwheelBlock> base,
                           Supplier<? extends MaterialEncasedCogwheelBlock> block, boolean brass) {
    }

    private static final List<Supplier<? extends MaterialCogwheelBlock>> BASES = new ArrayList<>();
    private static final List<Encased> ENCASED = new ArrayList<>();
    private static boolean hooksRegistered;
    private static boolean encasingRegistered;

    public static void addBase(Supplier<? extends MaterialCogwheelBlock> block) {
        BASES.add(Objects.requireNonNull(block));
    }

    public static void addEncased(Supplier<? extends MaterialCogwheelBlock> base,
                                 Supplier<? extends MaterialEncasedCogwheelBlock> block, boolean brass) {
        ENCASED.add(new Encased(Objects.requireNonNull(base), Objects.requireNonNull(block), brass));
    }

    public static List<Supplier<? extends MaterialCogwheelBlock>> baseBlocks() {
        return List.copyOf(BASES);
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE) || hooksRegistered) {
            return;
        }
        // Every block registry has finished by this point, including KubeJS custom namespaces.
        BASES.forEach(block -> BlockStressValues.IMPACTS.register(block.get(), () -> 0));
        ENCASED.forEach(entry -> BlockStressValues.IMPACTS.register(entry.block().get(), () -> 0));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ModCogwheels::registerClient);
        hooksRegistered = true;
    }

    @SuppressWarnings("removal")
    private static void registerClient() {
        BASES.forEach(block -> CreateRegistrate.blockModel(() -> BracketedKineticBlockModel::new)
                .accept(block.get()));
        for (Encased entry : ENCASED) {
            MaterialEncasedCogwheelBlock block = entry.block().get();
            ItemBlockRenderTypes.setRenderLayer(block, RenderType.cutoutMipped());
            CreateRegistrate.connectedTextures(() -> ModSpriteShifts.encasedCogwheel(entry.brass(), block.isLargeCog()))
                    .accept(block);
            CreateRegistrate.casingConnectivity((Block ignored, CasingConnectivity connectivity) -> connectivity.make(block,
                    entry.brass() ? AllSpriteShifts.BRASS_CASING : AllSpriteShifts.ANDESITE_CASING,
                    (state, face) -> face.getAxis() == state.getValue(EncasedCogwheelBlock.AXIS)
                            && !state.getValue(face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                            ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT)))
                    .accept(block);
        }
        ModPartialModels.init();
    }

    public static void registerEncasing() {
        if (encasingRegistered) {
            return;
        }
        for (Encased entry : ENCASED) {
            EncasingRegistry.addVariant(entry.base().get(), entry.block().get());
        }
        encasingRegistered = true;
    }

    private ModCogwheels() {
    }
}
