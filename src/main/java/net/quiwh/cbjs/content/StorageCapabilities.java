package net.quiwh.cbjs.content;

import com.simibubi.create.content.kinetics.belt.BeltBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.kubejs.ModFluidTanks;
import net.quiwh.cbjs.kubejs.ModItemVaults;
import net.quiwh.cbjs.mixin.NativeBasinCapabilitiesAccess;
import net.quiwh.cbjs.mixin.NativeBeltCapabilitiesAccess;
import net.quiwh.cbjs.mixin.NativeChuteCapabilitiesAccess;
import net.quiwh.cbjs.mixin.NativeTunnelCapabilitiesAccess;
import net.quiwh.cbjs.mixin.NativeBrassTunnelCapabilitiesAccess;

/** Create registers only its own exact types; scripted families need the same native providers. */
@EventBusSubscriber(modid = CBJS.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class StorageCapabilities {
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        ModFluidTanks.forEach((builder, type) -> event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK, type, (entity, side) -> entity.fluidHandler()));
        ModItemVaults.forEach((builder, type) -> event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK, type, (entity, side) -> entity.itemHandler()));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedLogisticsBlockEntities.CHUTE.get(),
                (entity, side) -> ((NativeChuteCapabilitiesAccess) entity).cbjs$getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedLogisticsBlockEntities.SMART_CHUTE.get(),
                (entity, side) -> ((NativeChuteCapabilitiesAccess) entity).cbjs$getItemHandler());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedLogisticsBlockEntities.BASIN.get(),
                (entity, side) -> ((NativeBasinCapabilitiesAccess) entity).cbjs$getItemCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ScriptedLogisticsBlockEntities.BASIN.get(),
                (entity, side) -> ((NativeBasinCapabilitiesAccess) entity).cbjs$getFluidCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedLogisticsBlockEntities.TUNNEL.get(),
                NativeTunnelCapabilitiesAccess::cbjs$getItemHandler);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedLogisticsBlockEntities.BRASS_TUNNEL.get(),
                (entity, side) -> ((NativeBrassTunnelCapabilitiesAccess) entity).cbjs$getTunnelCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ScriptedBeltBlockEntities.BELT.get(), (entity, side) -> {
            if (!BeltBlock.canTransportObjects(entity.getBlockState())) return null;
            var access = (NativeBeltCapabilitiesAccess) entity;
            if (!entity.isRemoved() && access.cbjs$getItemHandler() == null) access.cbjs$initializeItemHandler();
            return access.cbjs$getItemHandler();
        });
    }

    private StorageCapabilities() { }
}
