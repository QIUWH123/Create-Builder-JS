package net.quiwh.cbjs;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
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

    public CBJS(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();
        ModBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedShaftBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedBeltBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedGearboxBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedChainDriveBlockEntities.register();
        net.quiwh.cbjs.content.ScriptedTransmissionBlockEntities.register();
        CustomCasingBlockEntities.register();
        ModFluidPipeBlockEntities.register();
        ModFluidDeviceBlockEntities.register();
        net.quiwh.cbjs.content.ModFluidTankMountedStorage.register();
        net.quiwh.cbjs.content.ModItemVaultMountedStorage.register();
        if (net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
            net.quiwh.cbjs.compat.bnb.BnbChainCompat.register();
        REGISTRATE.registerEventListeners(modBus);
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
        if (net.quiwh.cbjs.compat.OptionalMods.bitsAndBobsLoaded())
            modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.compat.bnb.BnbChainCompat::onRegister);
        // Run after every dynamic registration above so storage families receive their aliases too.
        modBus.addListener(EventPriority.LOWEST, net.quiwh.cbjs.content.LegacyBlockEntityIds::onRegister);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(GearLimitManager::addReloadListener);
        MinecraftForge.EVENT_BUS.addListener(GearLimitManager::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.fuels.BlazeBurnerFuelManager::addReloadListener);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.fuels.BlazeBurnerFuelManager::serverStopped);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.kubejs.ModFluidTanks::resolveServerSettings);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.kubejs.ModItemVaults::resolveServerSettings);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.storage.StorageEventDispatcher::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.storage.StorageEventDispatcher::onServerStopped);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.belts.BeltEventDispatcher::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(net.quiwh.cbjs.belts.BeltEventDispatcher::onServerStopped);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModCogwheels::registerEncasing);
        event.enqueueWork(ModCasings::registerEncasing);
        event.enqueueWork(ModFluidPipes::registerEncasing);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModShafts::registerEncasing);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModFluidTanks::registerMovement);
        event.enqueueWork(net.quiwh.cbjs.kubejs.ModItemVaults::registerMovement);
    }
}
