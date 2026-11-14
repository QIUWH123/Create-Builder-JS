package net.quiwh.cbjs.client;

import com.simibubi.create.content.contraptions.bearing.BearingRenderer;
import com.simibubi.create.content.decoration.steamWhistle.WhistleRenderer;
import com.simibubi.create.content.fluids.pipes.TransparentStraightPipeRenderer;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogRenderer;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineRenderer;
import com.simibubi.create.content.logistics.chute.ChuteRenderer;
import com.simibubi.create.content.logistics.chute.SmartChuteRenderer;
import com.simibubi.create.content.logistics.funnel.FunnelRenderer;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelRenderer;
import com.simibubi.create.content.kinetics.transmission.SplitShaftRenderer;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerRenderer;
import com.simibubi.create.content.processing.basin.BasinRenderer;
import com.simibubi.create.content.processing.burner.BlazeBurnerRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.quiwh.cbjs.CBJS;
import net.quiwh.cbjs.content.CustomCasingBlockEntities;
import net.quiwh.cbjs.content.ModBlockEntities;
import net.quiwh.cbjs.content.ModFluidDeviceBlockEntities;
import net.quiwh.cbjs.content.ModFluidPipeBlockEntities;
import net.quiwh.cbjs.content.ScriptedArmGaugeBlockEntities;
import net.quiwh.cbjs.content.ScriptedBeltBlockEntities;
import net.quiwh.cbjs.content.ScriptedBlazeBurnerBlockEntities;
import net.quiwh.cbjs.content.ScriptedChainConveyorBlockEntities;
import net.quiwh.cbjs.content.ScriptedChainDriveBlockEntities;
import net.quiwh.cbjs.content.ScriptedGeneratorBlockEntities;
import net.quiwh.cbjs.content.ScriptedGearboxBlockEntities;
import net.quiwh.cbjs.content.ScriptedLogisticsBlockEntities;
import net.quiwh.cbjs.content.ScriptedShaftBlockEntities;
import net.quiwh.cbjs.content.ScriptedSteamBlockEntities;
import net.quiwh.cbjs.content.ScriptedTransmissionBlockEntities;

/** Registers native Create fallback renderers on the client only.
 *
 * Registrate's renderer builder method exposes Minecraft client types in its
 * signature. Keeping these calls out of common block-entity registration
 * classes prevents dedicated servers from resolving client classes.
 */
public final class BlockEntityRendererRegistration {
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.COGWHEEL.get(), MaterialCogwheelRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ENCASED_COGWHEEL.get(), MaterialCogwheelRenderer::new);
        event.registerBlockEntityRenderer(CustomCasingBlockEntities.ENCASED_SHAFT.get(), ShaftClientModels.ShaftRenderer::new);
        event.registerBlockEntityRenderer(CustomCasingBlockEntities.ENCASED_COGWHEEL.get(), EncasedCogRenderer::small);
        event.registerBlockEntityRenderer(CustomCasingBlockEntities.ENCASED_LARGE_COGWHEEL.get(), EncasedCogRenderer::large);
        event.registerBlockEntityRenderer(ModFluidDeviceBlockEntities.MECHANICAL_PUMP.get(), FluidDeviceClientModels.MaterialPumpRenderer::new);
        event.registerBlockEntityRenderer(ModFluidDeviceBlockEntities.SMART_FLUID_PIPE.get(), SmartBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModFluidDeviceBlockEntities.FLUID_VALVE.get(), FluidValveRenderer::new);
        event.registerBlockEntityRenderer(ModFluidPipeBlockEntities.GLASS_FLUID_PIPE.get(), TransparentStraightPipeRenderer::new);

        event.registerBlockEntityRenderer(ScriptedArmGaugeBlockEntities.MECHANICAL_ARM.get(), ArmRenderer::new);
        event.registerBlockEntityRenderer(ScriptedArmGaugeBlockEntities.FLYWHEEL.get(), ArmGaugeClientModels.FlywheelRenderer::new);
        event.registerBlockEntityRenderer(ScriptedArmGaugeBlockEntities.SPEEDOMETER.get(), ArmGaugeClientModels.GaugeRenderer::speed);
        event.registerBlockEntityRenderer(ScriptedArmGaugeBlockEntities.STRESSOMETER.get(), ArmGaugeClientModels.GaugeRenderer::stress);
        event.registerBlockEntityRenderer(ScriptedBeltBlockEntities.BELT.get(), BeltRenderer::new);
        event.registerBlockEntityRenderer(ScriptedBlazeBurnerBlockEntities.BURNER.get(), BlazeBurnerRenderer::new);
        event.registerBlockEntityRenderer(ScriptedChainConveyorBlockEntities.CONVEYOR.get(), ChainConveyorRenderer::new);
        event.registerBlockEntityRenderer(ScriptedChainDriveBlockEntities.CHAIN_DRIVE.get(), ChainDriveClientModels.ChainDriveRenderer::new);
        event.registerBlockEntityRenderer(ScriptedGearboxBlockEntities.GEARBOX.get(), GearboxRenderer::new);
        event.registerBlockEntityRenderer(ScriptedGearboxBlockEntities.SIX_WAY_GEARBOX.get(), DirectionalGearboxRenderer::new);
        event.registerBlockEntityRenderer(ScriptedGeneratorBlockEntities.WATER_WHEEL.get(), NativeGeneratorClientModels::smallWheelRenderer);
        event.registerBlockEntityRenderer(ScriptedGeneratorBlockEntities.LARGE_WATER_WHEEL.get(), NativeGeneratorClientModels::largeWheelRenderer);
        event.registerBlockEntityRenderer(ScriptedGeneratorBlockEntities.WINDMILL.get(), BearingRenderer::new);
        event.registerBlockEntityRenderer(ScriptedGeneratorBlockEntities.CRANK.get(), NativeGeneratorClientModels.CrankRenderer::new);
        event.registerBlockEntityRenderer(ScriptedGeneratorBlockEntities.MOTOR.get(), NativeGeneratorClientModels.MotorRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.CHUTE.get(), ChuteRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.SMART_CHUTE.get(), SmartChuteRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.FUNNEL.get(), FunnelRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.TUNNEL.get(), BeltTunnelRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.BRASS_TUNNEL.get(), BeltTunnelRenderer::new);
        event.registerBlockEntityRenderer(ScriptedLogisticsBlockEntities.BASIN.get(), BasinRenderer::new);
        event.registerBlockEntityRenderer(ScriptedShaftBlockEntities.SHAFT.get(), ShaftClientModels.ShaftRenderer::new);
        event.registerBlockEntityRenderer(ScriptedSteamBlockEntities.ENGINE.get(), SteamEngineRenderer::new);
        event.registerBlockEntityRenderer(ScriptedSteamBlockEntities.WHISTLE.get(), WhistleRenderer::new);
        event.registerBlockEntityRenderer(ScriptedSteamBlockEntities.POWERED_SHAFT.get(), ShaftClientModels.ShaftRenderer::new);
        event.registerBlockEntityRenderer(ScriptedTransmissionBlockEntities.CLUTCH.get(), SplitShaftRenderer::new);
        event.registerBlockEntityRenderer(ScriptedTransmissionBlockEntities.GEARSHIFT.get(), SplitShaftRenderer::new);
        event.registerBlockEntityRenderer(ScriptedTransmissionBlockEntities.SPEED_CONTROLLER.get(), SpeedControllerRenderer::new);
        CBJS.LOGGER.info("Registered CBJS native block-entity renderers");
    }

    private BlockEntityRendererRegistration() { }
}
