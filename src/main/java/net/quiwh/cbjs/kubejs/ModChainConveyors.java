package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.quiwh.cbjs.kubejs.ClientRegistration;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.ChainConveyorClientModels;

public final class ModChainConveyors {
    private static final List<ChainConveyorBuilder> BUILDERS = new ArrayList<>();
    private static boolean registered;
    static void add(ChainConveyorBuilder builder) { BUILDERS.add(builder); }
    public static void onRegister(RegisterEvent event) {
        if (registered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var builder : BUILDERS) BlockStressValues.IMPACTS.register(builder.get(),
                () -> BlockStressValues.getImpact(AllBlocks.CHAIN_CONVEYOR.get()));
        ClientRegistration.models(() -> () -> BUILDERS.forEach(ChainConveyorClientModels::declare));
        registered = true;
    }
    private ModChainConveyors() { }
}
