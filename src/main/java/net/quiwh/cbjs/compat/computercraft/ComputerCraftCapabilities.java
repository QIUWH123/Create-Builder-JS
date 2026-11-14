package net.quiwh.cbjs.compat.computercraft;

import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.compat.OptionalMods;
import net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities;
import net.quiwh.cbjs.content.ScriptedGeneratorBlockEntities;
import net.quiwh.cbjs.content.ScriptedTransmissionBlockEntities;

/** Native Create providers must also be registered for the scripted entity types. */
@EventBusSubscriber(modid = CBJS.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ComputerCraftCapabilities {
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        if (OptionalMods.computerCraftLoaded()) Providers.register(event);
    }

    /** Keep the optional API out of subscriber discovery and ordinary startup. */
    private static final class Providers {
        private static void register(RegisterCapabilitiesEvent event) {
            var peripheral = PeripheralCapability.get();
            event.registerBlockEntity(peripheral, ScriptedArmGaugeBlockEntities.SPEEDOMETER.get(),
                    (entity, side) -> entity.computerBehaviour.getPeripheralCapability());
            event.registerBlockEntity(peripheral, ScriptedArmGaugeBlockEntities.STRESSOMETER.get(),
                    (entity, side) -> entity.computerBehaviour.getPeripheralCapability());
            event.registerBlockEntity(peripheral, ScriptedTransmissionBlockEntities.SPEED_CONTROLLER.get(),
                    (entity, side) -> entity.computerBehaviour.getPeripheralCapability());
            event.registerBlockEntity(peripheral, ScriptedGeneratorBlockEntities.MOTOR.get(),
                    (entity, side) -> entity.computerBehaviour.getPeripheralCapability());
        }
    }

    private ComputerCraftCapabilities() {}
}
