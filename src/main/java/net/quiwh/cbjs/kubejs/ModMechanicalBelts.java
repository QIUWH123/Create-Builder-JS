package net.quiwh.cbjs.kubejs;

import com.simibubi.create.api.stress.BlockStressValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.MechanicalBeltClientModels;

/** Registration hooks only; native belts own transport and rendering lifecycles. */
public final class ModMechanicalBelts {
    private static final List<MechanicalBeltBuilder> BELTS = new ArrayList<>();
    private static boolean hooksRegistered;
    static void add(MechanicalBeltBuilder builder) { BELTS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (hooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var belt : BELTS) BlockStressValues.IMPACTS.register(belt.get(), () -> 0);
        ClientRegistration.models(() -> () -> BELTS.forEach(MechanicalBeltClientModels::declareModels));
        ClientRegistration.run(() -> () -> BELTS.forEach(MechanicalBeltClientModels::register));
        hooksRegistered = true;
    }
    private ModMechanicalBelts() { }
}
