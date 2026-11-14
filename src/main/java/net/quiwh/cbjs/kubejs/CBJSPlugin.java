package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.registry.BuilderTypeRegistry;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import net.minecraft.core.registries.Registries;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;

public final class CBJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBuilderTypes(BuilderTypeRegistry registry) {
        registry.of(Registries.ITEM, items -> {
        items.add("goggles", GogglesBuilder.class, GogglesBuilder::new);
        items.add("wrench", WrenchBuilder.class, WrenchBuilder::new);
        });
        registry.of(Registries.BLOCK, blocks -> {
        blocks.add("casing", CasingBuilder.class, CasingBuilder::new);
        blocks.add("shaft", ShaftBuilder.class, ShaftBuilder::new);
        blocks.add("encased_chain_drive", ChainDriveBuilder.class, ChainDriveBuilder::new);
        blocks.add("clutch", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.CLUTCH));
        blocks.add("gearshift", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.GEARSHIFT));
        blocks.add("rotation_speed_controller", TransmissionBuilder.class,
                id -> new TransmissionBuilder(id, TransmissionBuilder.Kind.SPEED_CONTROLLER));
        blocks.add("chain", ChainBuilder.class, ChainBuilder::new);
        blocks.add("chain_conveyor", ChainConveyorBuilder.class, ChainConveyorBuilder::new);
        blocks.add("mechanical_belt", MechanicalBeltBuilder.class, MechanicalBeltBuilder::new);
        blocks.add("gearbox", GearboxBuilder.class, id -> new GearboxBuilder(id, false));
        blocks.add("vertical_gearbox", GearboxBuilder.class, id -> new GearboxBuilder(id, true));
        blocks.add("six_way_gearbox", GearboxBuilder.class,
                id -> new GearboxBuilder(id, GearboxBuilder.Kind.SIX_WAY));
        blocks.add("fluid_pipe", FluidPipeBuilder.class, FluidPipeBuilder::new);
        blocks.add("fluid_tank", FluidTankBuilder.class, FluidTankBuilder::new);
        blocks.add("fluid_vessel", FluidTankBuilder.class, id -> new FluidTankBuilder(id, true));
        blocks.add("item_vault", ItemVaultBuilder.class, id -> new ItemVaultBuilder(id, false));
        blocks.add("item_silo", ItemVaultBuilder.class, id -> new ItemVaultBuilder(id, true));
        blocks.add("mechanical_pump", PumpBuilder.class, PumpBuilder::new);
        blocks.add("smart_pipe", SmartFluidPipeBuilder.class, SmartFluidPipeBuilder::new);
        blocks.add("fluid_valve", FluidValveBuilder.class, FluidValveBuilder::new);
        blocks.add("burner", BlazeBurnerBuilder.class, id -> new BlazeBurnerBuilder(id, true));
        blocks.add("blaze_burner", BlazeBurnerBuilder.class, BlazeBurnerBuilder::new);
        blocks.add("steam_engine", SteamEngineBuilder.class, SteamEngineBuilder::new);
        blocks.add("steam_whistle", SteamWhistleBuilder.class, SteamWhistleBuilder::new);
        blocks.add("mechanical_arm", MechanicalArmBuilder.class, MechanicalArmBuilder::new);
        blocks.add("chute", ChuteBuilder.class, id -> new ChuteBuilder(id, false));
        blocks.add("smart_chute", ChuteBuilder.class, id -> new ChuteBuilder(id, true));
        blocks.add("andesite_funnel", FunnelBuilder.class, id -> new FunnelBuilder(id, false));
        blocks.add("brass_funnel", FunnelBuilder.class, id -> new FunnelBuilder(id, true));
        blocks.add("andesite_tunnel", TunnelBuilder.class, id -> new TunnelBuilder(id, false));
        blocks.add("brass_tunnel", TunnelBuilder.class, id -> new TunnelBuilder(id, true));
        blocks.add("water_wheel", WaterWheelBuilder.class, id -> new WaterWheelBuilder(id, false));
        blocks.add("large_water_wheel", WaterWheelBuilder.class, id -> new WaterWheelBuilder(id, true));
        blocks.add("windmill_bearing", WindmillBearingBuilder.class, WindmillBearingBuilder::new);
        blocks.add("hand_crank", HandCrankBuilder.class, HandCrankBuilder::new);
        blocks.add("creative_motor", CreativeMotorBuilder.class, CreativeMotorBuilder::new);
        blocks.add("basin", BasinBuilder.class, BasinBuilder::new);
        blocks.add("flywheel", FlywheelBuilder.class, FlywheelBuilder::new);
        blocks.add("speedometer", GaugeBuilder.class, id -> new GaugeBuilder(id, false));
        blocks.add("stressometer", GaugeBuilder.class, id -> new GaugeBuilder(id, true));
        blocks.add("cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, false));
        blocks.add("large_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, true));
        blocks.add("shaftless_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, false, true));
        blocks.add("large_shaftless_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, true, true));
        blocks.add("flanged_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, false, false, true));
        blocks.add("large_flanged_cogwheel", CogwheelBuilder.class,
                id -> new CogwheelBuilder(id, true, false, true));
        });
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(CBJSEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry event) {
    }
}
