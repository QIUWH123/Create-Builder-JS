package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import com.simibubi.create.content.decoration.encasing.EncasableBlock;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.ShaftClientModels;
import java.util.ArrayList;
import java.util.List;

public final class ModShafts {
    private static final List<ShaftBuilder> SHAFTS = new ArrayList<>();
    private static final List<ShaftEncasedBuilder> ENCASED = new ArrayList<>();
    private static boolean hooksRegistered;
    private static boolean encasingRegistered;
    static void add(ShaftBuilder builder) { SHAFTS.add(builder); }
    static void add(ShaftEncasedBuilder builder) { ENCASED.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (hooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var shaft : SHAFTS) BlockStressValues.IMPACTS.register(shaft.get(), () -> 0);
        for (var shaft : ENCASED) BlockStressValues.IMPACTS.register(shaft.get(), () -> 0);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SHAFTS.forEach(ShaftClientModels::register));
        hooksRegistered = true;
    }
    public static void registerEncasing() {
        if (encasingRegistered) return;
        for (var variant : ENCASED) EncasingRegistry.addVariant((Block & EncasableBlock) variant.shaft.get(),
                (Block & EncasedBlock) variant.get());
        encasingRegistered = true;
    }
    private ModShafts() { }
}
