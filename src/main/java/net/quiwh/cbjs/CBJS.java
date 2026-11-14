package net.quiwh.cbjs;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.quiwh.cbjs.content.ModBlockEntities;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.ModCogwheels;
import net.quiwh.cbjs.content.ModFluidPipeBlockEntities;
import net.quiwh.cbjs.content.ModFluidDeviceBlockEntities;
import net.quiwh.cbjs.kubejs.ModCasings;
import net.quiwh.cbjs.kubejs.ModFluidPipes;
import net.quiwh.cbjs.kubejs.ModFluidDevices;
import net.quiwh.cbjs.kinetics.GearLimitManager;
import org.slf4j.Logger;

@Mod(CBJS.MOD_ID)
public final class CBJS {
    public static final String MOD_ID = "cbjs";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MOD_ID);

    public CBJS(IEventBus modBus) {
        // Attach Registrate before loading any static builders. Their visual and
        // native renderer hooks register themselves through the mod event bus.
        REGISTRATE.registerEventListeners(modBus);
        modBus.addListener(net.quiwh.cbjs.network.KineticStressSync::register);
        modBus.addListener(net.quiwh.cbjs.drives.DriveNetworking::register);
        net.quiwh.cbjs.drives.DriveEntities.register(modBus);
        ModBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedShaftBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedBeltBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedGearboxBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedChainDriveBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedTransmissionBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedBlazeBurnerBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedSteamBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedChainConveyorBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedLogisticsBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedGeneratorBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities.register();
        CustomCasingBlockEntities.register();
        ModFluidPipeBlockEntities.register();
        ModFluidDeviceBlockEntities.register();
        net.quiwh.cbjs.content.ModFluidTankMountedStorage.register();
        net.quiwh.cbjs.content.ModItemVaultMountedStorage.register();
        if (net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
            net.quiwh.cbjs.compat.bnb.BnbChainCompat.register();
        modBus.addListener(net.quiwh.cbjs.content.ScriptedBlockEntityValidity::apply);
        modBus.addListener(EventPriority.LOWEST, ModCogwheels::onRegister);
        modBus.addListener(EventPriority.LOWEST, ModCasings::onRegister);
        modBus.addListener(EventPriority.LOWEST, ModFluidPipes::onRegister);
        modBus.addListener(EventPriority.LOWEST, ModFluidDevices::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModFluidTanks::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModItemVaults::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModShafts::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModGearboxes::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModChainDrives::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModTransmissions::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModMechanicalBelts::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModBlazeBurners::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModSteamDevices::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModChainConveyors::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModLogistics::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModNativeGenerators::onRegister);
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.kubejs.ModArmGaugeMachines::onRegister);
        if (net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
            modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.compat.bnb.BnbChainCompat::onRegister);
        // Run after every dynamic registration above so storage families receive their aliases too.
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.content.LegacyBlockEntityIds::onRegister);
        modBus.addListener(this::commonSetup);
        modBus.addListener((FMLClientSetupEvent event) -> net.quiwh.cbjs.kubejs.ClientRegistration.onClientSetup(event));
        modBus.addListener(EventPriority.HIGHEST, net.quiwh.cbjs.kubejs.ClientRegistration::onRegisterAdditional);
        modBus.addListener(net.quiwh.cbjs.client.FluidTankClientRegistration::registerRenderers);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.drives.DriveInteraction::onRightClick);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.drives.DriveInteraction::loggedOut);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.drives.DriveInteraction::changedDimension);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.drives.DriveInteraction::stopped);
        NeoForge.EVENT_BUS.addListener(GearLimitManager::addReloadListener);
        NeoForge.EVENT_BUS.addListener(GearLimitManager::serverStopped);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.network.KineticStressSync::playerLoggedIn);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.fuels.BlazeBurnerFuelManager::addReloadListener);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.fuels.BlazeBurnerFuelManager::serverStopped);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.kubejs.ModFluidTanks::resolveServerSettings);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.kubejs.ModItemVaults::resolveServerSettings);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.storage.StorageEventDispatcher::onServerTick);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.storage.StorageEventDispatcher::onServerStopped);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.belts.BeltEventDispatcher::onServerTick);
        NeoForge.EVENT_BUS.addListener(net.quiwh.cbjs.belts.BeltEventDispatcher::onServerStopped);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModLogistics::registerMovement);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModArmGaugeMachines::registerDisplaySources);
        event.enqueueWork(ModCogwheels::registerEncasing);
        event.enqueueWork(ModCasings::registerEncasing);
        event.enqueueWork(ModFluidPipes::registerEncasing);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModShafts::registerEncasing);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModFluidTanks::registerMovement);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModItemVaults::registerMovement);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModBlazeBurners::registerMovement);
    }
}
