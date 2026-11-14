package net.quiwh.cbjs.kubejs;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.model.baked.PartialModelEventHandler;
import java.util.HashSet;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Regression checks for client model declaration timing and dedicated-server safety. */
public final class ClientRegistrationTest {
    private static int checks;

    public static void main(String[] args) {
        clientDeclarationsWaitForModelRegistrationBoundary();
        dedicatedServerDoesNotEvaluateClientDeclarations();
        clientSetupQueueDrainsAfterFailure();
        System.out.println("ClientRegistrationTest: " + checks + " checks passed");
    }

    private static void clientDeclarationsWaitForModelRegistrationBoundary() {
        StringBuilder calls = new StringBuilder();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("cbjs_test", "block/moving_part");
        ClientRegistration.models(Dist.CLIENT, () -> () -> {
            PartialModel.of(id);
            calls.append("registered");
        });
        expect("", calls.toString(), "client declaration waits until registration boundary");
        var models = new HashSet<ModelResourceLocation>();
        var additional = new ModelEvent.RegisterAdditional(models);
        ClientRegistration.onRegisterAdditional(Dist.CLIENT, additional);
        PartialModelEventHandler.onRegisterAdditional(additional);
        expect("registered", calls.toString(), "flush executes the queued declaration");
        expect(true, models.contains(ModelResourceLocation.standalone(id)),
                "Flywheel sees a scripted partial during its RegisterAdditional scan");

    }

    private static void dedicatedServerDoesNotEvaluateClientDeclarations() {
        StringBuilder calls = new StringBuilder();
        ClientRegistration.run(Dist.DEDICATED_SERVER, () -> {
            calls.append("evaluated");
            return () -> calls.append("executed");
        });
        expect("", calls.toString(), "dedicated server never evaluates client declaration");
    }

    private static void clientSetupQueueDrainsAfterFailure() {
        StringBuilder calls = new StringBuilder();
        ClientRegistration.run(Dist.CLIENT, () -> () -> { throw new IllegalStateException("first"); });
        ClientRegistration.run(Dist.CLIENT, () -> () -> calls.append("second"));
        try {
            ClientRegistration.onClientSetup(Dist.CLIENT);
            throw new AssertionError("client setup should report the first callback failure");
        } catch (IllegalStateException expected) {
            // The queue must continue draining before reporting the first failure.
        }
        expect("second", calls.toString(), "client setup continues after a failed hook");
        ClientRegistration.run(Dist.CLIENT, () -> () -> calls.append("late"));
        expect("secondlate", calls.toString(), "hooks registered after client setup run immediately");
    }

    private static void expect(String expected, String actual, String description) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }

    private static void expect(boolean expected, boolean actual, String description) {
        checks++;
        if (expected != actual) throw new AssertionError(description + ": expected " + expected + ", got " + actual);
    }

    private ClientRegistrationTest() { }
}
