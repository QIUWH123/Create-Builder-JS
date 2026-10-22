package net.quiwh.cbjs.kinetics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/** Standalone cache and mutable-map contracts; run with Java 17 and no game dependencies. */
public final class NetworkStressSnapshotTest {
    private static int checks;

    public static void main(String[] args) {
        unchangedDemandCalculatesOnce();
        invalidationRefreshesImmediately();
        nativeCalculationSuppliesTheNextRead();
        zeroDemandIsAValidCachedResult();
        failedCalculationCanBeRetried();
        directMapChangesInvalidateDemand();
        mapViewChangesInvalidateDemand();
        defaultMapChangesInvalidateDemand();
        readingRetainedViewsKeepsTheCacheFresh();
        System.out.println("NetworkStressSnapshotTest: " + checks + " checks passed");
    }

    private static void unchangedDemandCalculatesOnce() {
        int[] calculations = {0};
        var snapshot = new NetworkStressSnapshot(() -> {
            calculations[0]++;
            return 8192;
        });
        for (int i = 0; i < 10_000; i++) {
            equal(8192, snapshot.get(), "Unchanged demand");
        }
        equal(1, calculations[0], "Ten thousand reads share one network calculation");
    }

    private static void invalidationRefreshesImmediately() {
        int[] calculations = {0};
        float[] demand = {8192};
        var snapshot = new NetworkStressSnapshot(() -> {
            calculations[0]++;
            return demand[0];
        });
        equal(8192, snapshot.get(), "Initial demand");
        demand[0] = 4096;
        snapshot.invalidate();
        snapshot.invalidate();
        equal(4096, snapshot.get(), "Machine removal is visible to the next same-tick check");
        equal(2, calculations[0], "Repeated invalidations coalesce before a read");
        demand[0] = 12288;
        snapshot.invalidate();
        equal(12288, snapshot.get(), "A later same-tick mutation refreshes again");
        equal(3, calculations[0], "Each observed mutation refreshes demand");
        equal(12288, snapshot.get(), "Refreshed demand remains cached");
        equal(3, calculations[0], "An unchanged post-callback recheck does not scan");
    }

    private static void nativeCalculationSuppliesTheNextRead() {
        int[] calculations = {0};
        var snapshot = new NetworkStressSnapshot(() -> {
            calculations[0]++;
            return 64;
        });
        snapshot.record(512);
        equal(512, snapshot.get(), "Native Create calculation initializes the snapshot");
        equal(0, calculations[0], "Recorded demand avoids a duplicate scan");
        snapshot.invalidate();
        snapshot.record(1024);
        equal(1024, snapshot.get(), "Native recomputation satisfies pending invalidation");
        equal(0, calculations[0], "Recording dirty demand avoids a second calculation");
        snapshot.record(2048);
        equal(2048, snapshot.get(), "A native result replaces an already fresh value");
        snapshot.invalidate();
        equal(64, snapshot.get(), "Mutation after recording still requires a fresh calculation");
        equal(1, calculations[0], "Only demand after the last invalidation is calculated");
    }

    private static void zeroDemandIsAValidCachedResult() {
        int[] calculations = {0};
        var snapshot = new NetworkStressSnapshot(() -> {
            calculations[0]++;
            return 0;
        });
        for (int i = 0; i < 1000; i++) equal(0, snapshot.get(), "Zero-demand network");
        equal(1, calculations[0], "Zero is cached instead of treated as uninitialized");
        snapshot.invalidate();
        snapshot.record(0);
        equal(0, snapshot.get(), "Native zero demand remains fresh");
        equal(1, calculations[0], "Recorded zero avoids recalculation");
    }

    private static void failedCalculationCanBeRetried() {
        int[] calculations = {0};
        var snapshot = new NetworkStressSnapshot(() -> {
            if (++calculations[0] == 1) throw new IllegalStateException("expected calculation failure");
            return 128;
        });
        try {
            snapshot.get();
            throw new AssertionError("Calculation failure was swallowed");
        } catch (IllegalStateException expected) {
            equal("expected calculation failure", expected.getMessage(), "Original failure propagates");
        }
        equal(128, snapshot.get(), "Failed demand calculation remains eligible for retry");
        equal(2, calculations[0], "Calculation retries once after failure");
        equal(128, snapshot.get(), "Successful retry is cached");
        equal(2, calculations[0], "Retry success does not scan again");
    }

    private static void directMapChangesInvalidateDemand() {
        mutation("put new member", map -> map.put("d", 40), expected("a", 10, "b", 20, "c", 30, "d", 40));
        mutation("put new impact", map -> map.put("a", 11), expected("a", 11, "b", 20, "c", 30));
        mutation("remove member", map -> map.remove("b"), expected("a", 10, "c", 30));
        mutation("putAll impacts", map -> map.putAll(Map.of("a", 12, "d", 40)),
                expected("a", 12, "b", 20, "c", 30, "d", 40));
        mutation("clear members", Map::clear, Map.of());
    }

    private static void mapViewChangesInvalidateDemand() {
        mutation("keySet removal", map -> map.keySet().remove("b"), expected("a", 10, "c", 30));
        mutation("values removal", map -> map.values().remove(20), expected("a", 10, "c", 30));
        mutation("entrySet removal", map -> map.entrySet().remove(Map.entry("b", 20)),
                expected("a", 10, "c", 30));
        mutation("key iterator removal", map -> {
            var iterator = map.keySet().iterator();
            iterator.next();
            iterator.remove();
        }, expected("b", 20, "c", 30));
        mutation("value iterator removal", map -> {
            var iterator = map.values().iterator();
            iterator.next();
            iterator.remove();
        }, expected("b", 20, "c", 30));
        mutation("entry iterator removal", map -> {
            var iterator = map.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }, expected("b", 20, "c", 30));
        mutation("entry setValue", map -> map.entrySet().iterator().next().setValue(11),
                expected("a", 11, "b", 20, "c", 30));
        mutation("entry from array setValue", map -> {
            @SuppressWarnings("unchecked")
            Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) map.entrySet().toArray()[0];
            entry.setValue(11);
        }, expected("a", 11, "b", 20, "c", 30));
        mutation("entry from stream setValue", map -> map.entrySet().stream().findFirst().orElseThrow().setValue(11),
                expected("a", 11, "b", 20, "c", 30));
        mutation("keySet removeAll", map -> map.keySet().removeAll(Set.of("a", "c")), expected("b", 20));
        mutation("keySet retainAll", map -> map.keySet().retainAll(Set.of("b")), expected("b", 20));
        mutation("keySet removeIf", map -> map.keySet().removeIf(key -> key.equals("b")),
                expected("a", 10, "c", 30));
        mutation("values removeAll", map -> map.values().removeAll(Set.of(10, 30)), expected("b", 20));
        mutation("values retainAll", map -> map.values().retainAll(Set.of(20)), expected("b", 20));
        mutation("values removeIf", map -> map.values().removeIf(value -> value >= 20), expected("a", 10));
        mutation("entrySet removeIf", map -> map.entrySet().removeIf(entry -> entry.getKey().equals("b")),
                expected("a", 10, "c", 30));
        mutation("entrySet retainAll", map -> map.entrySet().retainAll(Set.of(Map.entry("b", 20))),
                expected("b", 20));
        mutation("keySet clear", map -> map.keySet().clear(), Map.of());
        mutation("values clear", map -> map.values().clear(), Map.of());
        mutation("entrySet clear", map -> map.entrySet().clear(), Map.of());
    }

    private static void defaultMapChangesInvalidateDemand() {
        mutation("putIfAbsent", map -> map.putIfAbsent("d", 40),
                expected("a", 10, "b", 20, "c", 30, "d", 40));
        mutation("conditional remove", map -> map.remove("b", 20), expected("a", 10, "c", 30));
        mutation("replace impact", map -> map.replace("b", 21), expected("a", 10, "b", 21, "c", 30));
        mutation("conditional replace", map -> map.replace("b", 20, 21),
                expected("a", 10, "b", 21, "c", 30));
        mutation("replaceAll impacts", map -> map.replaceAll((key, value) -> value + 1),
                expected("a", 11, "b", 21, "c", 31));
        mutation("computeIfAbsent", map -> map.computeIfAbsent("d", key -> 40),
                expected("a", 10, "b", 20, "c", 30, "d", 40));
        mutation("computeIfPresent", map -> map.computeIfPresent("b", (key, value) -> value + 1),
                expected("a", 10, "b", 21, "c", 30));
        mutation("computeIfPresent removal", map -> map.computeIfPresent("b", (key, value) -> null),
                expected("a", 10, "c", 30));
        mutation("compute impact", map -> map.compute("b", (key, value) -> value + 1),
                expected("a", 10, "b", 21, "c", 30));
        mutation("compute removal", map -> map.compute("b", (key, value) -> null), expected("a", 10, "c", 30));
        mutation("merge impact", map -> map.merge("b", 1, Integer::sum),
                expected("a", 10, "b", 21, "c", 30));
        mutation("merge insertion", map -> map.merge("d", 40, Integer::sum),
                expected("a", 10, "b", 20, "c", 30, "d", 40));
        mutation("merge removal", map -> map.merge("b", 1, (old, added) -> null),
                expected("a", 10, "c", 30));
    }

    private static void readingRetainedViewsKeepsTheCacheFresh() {
        Fixture fixture = new Fixture();
        var keys = fixture.members.keySet();
        var values = fixture.members.values();
        var entries = fixture.members.entrySet();
        equal(60, fixture.snapshot.get(), "Initial view test demand");
        for (int i = 0; i < 1000; i++) {
            equal(10, fixture.members.get("a").intValue(), "Read impact");
            require(fixture.members.containsKey("b") && fixture.members.containsValue(30), "Contains queries");
            require(keys.contains("c") && values.contains(20) && entries.contains(Map.entry("a", 10)),
                    "View contains queries");
            equal(3, fixture.members.size(), "Read network size");
            equal(60, fixture.snapshot.get(), "Read operations preserve cached demand");
        }
        equal(1, fixture.calculations, "Map and view reads do not invalidate demand");
        equal(0, fixture.invalidations, "Reading members does not notify mutations");
    }

    private static void mutation(String description, Consumer<Map<String, Integer>> mutate,
                                 Map<String, Integer> expected) {
        Fixture fixture = new Fixture();
        Set<String> keys = fixture.members.keySet();
        Collection<Integer> values = fixture.members.values();
        Set<Map.Entry<String, Integer>> entries = fixture.members.entrySet();
        equal(60, fixture.snapshot.get(), description + ": initial demand");
        mutate.accept(fixture.members);
        require(fixture.invalidations > 0, description + ": mutation invalidates the snapshot");
        equal(expected, fixture.delegate, description + ": original map changes");
        equal(expected, fixture.members, description + ": wrapped map changes");
        equal(expected.hashCode(), fixture.members.hashCode(), description + ": map hashCode");
        equal(expected.keySet(), new HashSet<>(keys), description + ": retained keySet stays live");
        equal(sorted(expected.values()), sorted(values), description + ": retained values stay live");
        equal(expected.entrySet(), entries, description + ": retained entrySet stays live");
        float demand = expected.values().stream().mapToInt(Integer::intValue).sum();
        equal(demand, fixture.snapshot.get(), description + ": next check sees new demand");
        equal(2, fixture.calculations, description + ": mutation causes one additional calculation");
        for (int i = 0; i < 1000; i++) equal(demand, fixture.snapshot.get(), description + ": unchanged demand");
        equal(2, fixture.calculations, description + ": repeated checks reuse refreshed demand");
    }

    private static List<Integer> sorted(Collection<Integer> values) {
        var result = new ArrayList<>(values);
        result.sort(Integer::compareTo);
        return result;
    }

    private static Map<String, Integer> expected(Object... entries) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], (Integer) entries[i + 1]);
        return map;
    }

    private static final class Fixture {
        final Map<String, Integer> delegate = new LinkedHashMap<>();
        final NetworkStressSnapshot snapshot;
        final StressTrackedMap<String, Integer> members;
        int calculations;
        int invalidations;

        Fixture() {
            // Explicit insertion order makes view-iterator mutations deterministic.
            delegate.put("a", 10);
            delegate.put("b", 20);
            delegate.put("c", 30);
            snapshot = new NetworkStressSnapshot(() -> {
                calculations++;
                return delegate.values().stream().mapToInt(Integer::intValue).sum();
            });
            members = new StressTrackedMap<>(delegate, () -> {
                invalidations++;
                snapshot.invalidate();
            });
        }
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void equal(float expected, float actual, String description) {
        checks++;
        if (Float.compare(expected, actual) != 0) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }

    private static void equal(Object expected, Object actual, String description) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(description + ": expected " + expected + ", got " + actual);
        }
    }
}
