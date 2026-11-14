package net.quiwh.cbjs.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Keeps native Create 6.0.10 visual registration policies aligned with the
 * block-entity families that retain supplementary BER rendering.
 */
public final class NativeVisualRegistrationTest {
    private static int checks;

    public static void main(String[] args) throws IOException {
        String conveyor = source("src/main/java/net/quiwh/cbjs/content/ScriptedChainConveyorBlockEntities.java");
        String armGauge = source("src/main/java/net/quiwh/cbjs/content/ScriptedArmGaugeBlockEntities.java");
        String mod = source("src/main/java/net/quiwh/cbjs/CBJS.java");

        expectContains(conveyor, ".visual(() -> ChainConveyorVisual::new, false)", "chain conveyor keeps its render predicate");
        expectContains(armGauge, ".visual(() -> ArmVisual::new, false)", "mechanical arm keeps its render predicate");
        expectContains(armGauge, ".visual(() -> ArmGaugeClientModels.SpeedVisual::new, false)", "speedometer keeps its render predicate");
        expectContains(armGauge, ".visual(() -> ArmGaugeClientModels.StressVisual::new, false)", "stressometer keeps its render predicate");
        expectContains(conveyor, ".renderer(() -> ChainConveyorRenderer::new)", "chain conveyor uses Registrate BER registration");
        expectContains(armGauge, ".renderer(() -> ArmRenderer::new)", "mechanical arm uses Registrate BER registration");
        expectContains(armGauge, ".renderer(() -> ArmGaugeClientModels.GaugeRenderer::speed)", "speedometer uses Registrate BER registration");
        expectContains(armGauge, ".renderer(() -> ArmGaugeClientModels.GaugeRenderer::stress)", "stressometer uses Registrate BER registration");
        expectContains(mod, "FluidTankClientRegistration::registerRenderers", "dynamic tank BER handler is attached directly to the client mod bus");
        expectNotContains(mod, "BlockEntityRendererRegistration::register", "fixed BERs are not registered a second time");
        for (String file : new String[] {"CustomCasingBlockEntities", "ModBlockEntities", "ModFluidDeviceBlockEntities",
                "ModFluidPipeBlockEntities", "ScriptedArmGaugeBlockEntities", "ScriptedBeltBlockEntities",
                "ScriptedBlazeBurnerBlockEntities", "ScriptedChainConveyorBlockEntities", "ScriptedChainDriveBlockEntities",
                "ScriptedGearboxBlockEntities", "ScriptedGeneratorBlockEntities", "ScriptedLogisticsBlockEntities",
                "ScriptedShaftBlockEntities", "ScriptedSteamBlockEntities", "ScriptedTransmissionBlockEntities"}) {
            String registration = source("src/main/java/net/quiwh/cbjs/content/" + file + ".java");
            expectContains(registration, ".renderer(() ->", file + " retains Registrate BER registration");
        }
        System.out.println("NativeVisualRegistrationTest: " + checks + " checks passed");
    }

    private static String source(String relative) throws IOException {
        return Files.readString(Path.of(relative));
    }

    private static void expectContains(String source, String expected, String description) {
        checks++;
        if (!source.contains(expected)) throw new AssertionError(description + ": missing " + expected);
    }

    private static void expectNotContains(String source, String unexpected, String description) {
        checks++;
        if (source.contains(unexpected)) throw new AssertionError(description + ": found " + unexpected);
    }

    private NativeVisualRegistrationTest() { }
}
