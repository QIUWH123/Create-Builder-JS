package net.quiwh.cbjs.content;

import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveVisual;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.builders.BlockEntityBuilder;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import net.minecraft.world.level.block.Block;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.client.FluidDeviceClientModels;

import java.util.function.Supplier;

/** Native Create transport, filtering, and kinetic ticking for script-registered devices. */
public final class ModFluidDeviceBlockEntities {
    private static final BlockEntityBuilder<PumpBlockEntity, CreateRegistrate> PUMP_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_mechanical_pump", PumpBlockEntity::new)
            .visual(() -> FluidDeviceClientModels.MaterialPumpVisual::new)
            .renderer(() -> FluidDeviceClientModels.MaterialPumpRenderer::new);
    private static final BlockEntityBuilder<SmartFluidPipeBlockEntity, CreateRegistrate> SMART_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_smart_fluid_pipe", SmartFluidPipeBlockEntity::new)
            .renderer(() -> SmartBlockEntityRenderer::new);
    private static final BlockEntityBuilder<FluidValveBlockEntity, CreateRegistrate> VALVE_BUILDER = CBJS.REGISTRATE
            .blockEntity("custom_fluid_valve", FluidValveBlockEntity::new)
            .visual(() -> FluidValveVisual::new)
            .renderer(() -> FluidValveRenderer::new);

    public static final BlockEntityEntry<PumpBlockEntity> MECHANICAL_PUMP = PUMP_BUILDER.register();
    public static final BlockEntityEntry<SmartFluidPipeBlockEntity> SMART_FLUID_PIPE = SMART_BUILDER.register();
    public static final BlockEntityEntry<FluidValveBlockEntity> FLUID_VALVE = VALVE_BUILDER.register();

    public static void addPump(Supplier<? extends Block> block) { PUMP_BUILDER.validBlock(block::get); }
    public static void addSmartPipe(Supplier<? extends Block> block) { SMART_BUILDER.validBlock(block::get); }
    public static void addValve(Supplier<? extends Block> block) { VALVE_BUILDER.validBlock(block::get); }
    public static void register() { }
    private ModFluidDeviceBlockEntities() { }
}
