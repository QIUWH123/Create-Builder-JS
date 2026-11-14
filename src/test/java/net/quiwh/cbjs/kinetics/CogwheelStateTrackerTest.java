package net.quiwh.cbjs.kinetics;

/** Standalone snapshot semantics without a Minecraft runtime. */
public final class CogwheelStateTrackerTest {
    private static int checks;

    public static void main(String[] args) {
        var tracker = new CogwheelStateTracker();
        var idle = state(0, 0, 0, 0, false);
        var previous = tracker.observe(idle);
        expect(!idle.speedChangedFrom(previous) && !idle.stressChangedFrom(previous), "Initial idle is silent");
        var running = state(64, 64, 10, 100, false);
        previous = tracker.observe(running);
        expect(running.speedChangedFrom(previous), "First active observation reports startup");
        expect(running.stressChangedFrom(previous), "First network observation reports stress/capacity");
        var reverse = state(-64, -64, 10, 100, false);
        expect(reverse.speedChangedFrom(running), "Equal magnitude reversal changes speed");
        var stalled = state(0, 64, 10, 100, true);
        expect(stalled.speedChangedFrom(running), "Stall changes actual speed");
        expect(stalled.stressChangedFrom(running), "Overstressed alone changes stress state");
        expect(state(0, 128, 10, 100, true).speedChangedFrom(stalled), "Theoretical speed changes while stalled");
        expect(state(64, 64, 11, 100, false).stressChangedFrom(running), "Stress alone changes");
        expect(state(64, 64, 10, 101, false).stressChangedFrom(running), "Capacity alone changes");
        expect(!running.speedChangedFrom(running) && !running.stressChangedFrom(running), "Repeated state is silent");
        expect(idle.speedChangedFrom(running), "Removal or disconnect reports stop");
        var signedZero = state(-0.0f, -0.0f, -0.0f, -0.0f, false);
        expect(!signedZero.speedChangedFrom(idle) && !signedZero.stressChangedFrom(idle), "Signed zero is equivalent");
        previous = tracker.observe(reverse);
        expect(previous == running, "observe returns the prior immutable snapshot");
        expect(tracker.observe(reverse) == reverse, "Current advances before a consumer can observe again");
        expect(previous.rpm() == 64 && previous.capacity() == 100, "Old snapshot remains unchanged");
        expect(!state(64, 64, 11, 101, true).speedChangedFrom(running), "Stress changes do not masquerade as speed changes");
        expect(!reverse.stressChangedFrom(running), "Reversal does not masquerade as stress change");
        var stableTracker = new CogwheelStateTracker();
        stableTracker.observe(running);
        stableTracker.observe(state(64, 64, 10, 100, false));
        expect(stableTracker.observe(idle) == running, "Unchanged values reuse the prior snapshot");
        var nan = state(Float.NaN, Float.NaN, Float.NaN, Float.NaN, false);
        stableTracker.observe(nan);
        stableTracker.observe(state(Float.NaN, Float.NaN, Float.NaN, Float.NaN, false));
        expect(stableTracker.observe(idle) == nan, "Repeated NaN values reuse the prior snapshot");
        primitiveObservationsReuseSnapshots();
        System.out.println("CogwheelStateTrackerTest: " + checks + " checks passed");
    }

    private static void primitiveObservationsReuseSnapshots() {
        var tracker = new CogwheelStateTracker();
        expect(tracker.observe(-0.0f, 0, -0.0f, 0, false) == CogwheelKineticState.ZERO
                        && tracker.current() == CogwheelKineticState.ZERO,
                "Primitive idle observation reuses zero snapshot");
        tracker.observe(64, 64, 10, 100, false);
        var running = tracker.current();
        expect(running.rpm() == 64 && running.theoreticalRpm() == 64 && running.stress() == 10
                        && running.capacity() == 100 && !running.overstressed(),
                "Primitive observation publishes all values before callbacks");
        expect(tracker.observe(64, 64, 10, 100, false) == running && tracker.current() == running,
                "Unchanged primitive observation retains snapshot");
        expect(tracker.observe(-64, 64, 10, 100, false) == running && tracker.current().rpm() == -64,
                "Primitive RPM reversal advances snapshot");
        tracker.observe(-64, 128, 10, 100, false);
        expect(tracker.current().theoreticalRpm() == 128, "Primitive theoretical RPM advances snapshot");
        tracker.observe(-64, 128, 11, 100, false);
        expect(tracker.current().stress() == 11, "Primitive stress advances snapshot");
        tracker.observe(-64, 128, 11, 101, false);
        expect(tracker.current().capacity() == 101, "Primitive capacity advances snapshot");
        tracker.observe(-64, 128, 11, 101, true);
        expect(tracker.current().overstressed(), "Primitive overstress advances snapshot");
        tracker.observe(Float.NaN, Float.NaN, Float.NaN, Float.NaN, false);
        var nan = tracker.current();
        expect(tracker.observe(Float.NaN, Float.NaN, Float.NaN, Float.NaN, false) == nan
                        && tracker.current() == nan, "Repeated primitive NaN reuses snapshot");
    }

    private static CogwheelKineticState state(float rpm, float theoretical, float stress, float capacity, boolean overstressed) {
        return new CogwheelKineticState(rpm, theoretical, stress, capacity, overstressed);
    }

    private static void expect(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
