package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.RegistryInfo;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

public final class CBJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        RegistryInfo.ITEM.addType("goggles", GogglesBuilder.class, GogglesBuilder::new);
        RegistryInfo.ITEM.addType("wrench", WrenchBuilder.class, WrenchBuilder::new);
        RegistryInfo.BLOCK.addType("casing", CasingBuilder.class, CasingBuilder::new);
        RegistryInfo.BLOCK.addType("shaft", ShaftBuilder.class, ShaftBuilder::new);
        RegistryInfo.BLOCK.addType("encased_chain_drive", ChainDriveBuilder.class, ChainDriveBuilder::new);
        RegistryInfo.BLOCK.addType("clutch", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.CLUTCH));
        RegistryInfo.BLOCK.addType("gearshift", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.GEARSHIFT));
        RegistryInfo.BLOCK.addType("rotation_speed_controller", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.SPEED_CONTROLLER));
        RegistryInfo.BLOCK.addType("chain", ChainBuilder.class, ChainBuilder::new);
        RegistryInfo.BLOCK.addType("mechanical_belt", MechanicalBeltBuilder.class, MechanicalBeltBuilder::new);
        RegistryInfo.BLOCK.addType("gearbox", GearboxBuilder.class, id -> new GearboxBuilder(id, false));
        RegistryInfo.BLOCK.addType("vertical_gearbox", GearboxBuilder.class, id -> new GearboxBuilder(id, true));
        RegistryInfo.BLOCK.addType("six_way_gearbox", GearboxBuilder.class,
                id -> new GearboxBuilder(id, GearboxBuilder.Kind.SIX_WAY));
        RegistryInfo.BLOCK.addType("fluid_pipe", FluidPipeBuilder.class, FluidPipeBuilder::new);
        RegistryInfo.BLOCK.addType("fluid_tank", FluidTankBuilder.class, FluidTankBuilder::new);
        RegistryInfo.BLOCK.addType("fluid_vessel", FluidTankBuilder.class, id -> new FluidTankBuilder(id, true));
        RegistryInfo.BLOCK.addType("item_vault", ItemVaultBuilder.class, id -> new ItemVaultBuilder(id, false));
        RegistryInfo.BLOCK.addType("item_silo", ItemVaultBuilder.class, id -> new ItemVaultBuilder(id, true));
        RegistryInfo.BLOCK.addType("mechanical_pump", PumpBuilder.class, PumpBuilder::new);
        RegistryInfo.BLOCK.addType("smart_pipe", SmartFluidPipeBuilder.class, SmartFluidPipeBuilder::new);
        RegistryInfo.BLOCK.addType("fluid_valve", FluidValveBuilder.class, FluidValveBuilder::new);
        RegistryInfo.BLOCK.addType("cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, false));
        RegistryInfo.BLOCK.addType("large_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, true));
        RegistryInfo.BLOCK.addType("shaftless_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, false, true));
        RegistryInfo.BLOCK.addType("large_shaftless_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, true, true));
    }

    @Override
    public void registerEvents() {
        CBJSEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        if (event.getType() == ScriptType.SERVER) CBJSProbeJS.register();
    }
}
