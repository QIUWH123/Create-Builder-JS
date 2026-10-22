package net.quiwh.cbjs.kubejs;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.quiwh.cbjs.client.FluidDeviceClientModels;
import net.quiwh.cbjs.content.ModFluidDeviceBlockEntities;

/** Resolves native transport, stress and client hooks after script block registration. */
public final class ModFluidDevices {
    private static final List<PumpBuilder> PUMPS = new ArrayList<>();
    private static final List<SmartFluidPipeBuilder> SMART_PIPES = new ArrayList<>();
    private static final List<FluidValveBuilder> VALVES = new ArrayList<>();
    private static boolean hooksRegistered;

    static void add(PumpBuilder builder) {
        PUMPS.add(builder);
        ModFluidDeviceBlockEntities.addPump(builder);
    }

    static void add(SmartFluidPipeBuilder builder) {
        SMART_PIPES.add(builder);
        ModFluidDeviceBlockEntities.addSmartPipe(builder);
    }

    static void add(FluidValveBuilder builder) {
        VALVES.add(builder);
        ModFluidDeviceBlockEntities.addValve(builder);
    }

    public static void onRegister(RegisterEvent event) {
        if (hooksRegistered || !event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var pump : PUMPS)
            BlockStressValues.IMPACTS.register(pump.get(), () -> BlockStressValues.getImpact(AllBlocks.MECHANICAL_PUMP.get()));
        for (var valve : VALVES)
            BlockStressValues.IMPACTS.register(valve.get(), () -> BlockStressValues.getImpact(AllBlocks.FLUID_VALVE.get()));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            for (var pump : PUMPS) FluidDeviceClientModels.register(pump);
            for (var pipe : SMART_PIPES) FluidDeviceClientModels.register(pipe);
            for (var valve : VALVES) FluidDeviceClientModels.register(valve);
        });
        hooksRegistered = true;
    }

    private ModFluidDevices() { }
}
