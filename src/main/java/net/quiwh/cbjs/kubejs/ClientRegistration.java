package net.quiwh.cbjs.kubejs;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Defers script-specific client models until registries are complete and before model baking. */
public final class ClientRegistration {
    private static final Object LOCK = new Object();
    private static final Queue<Supplier<Runnable>> PENDING_SETUP = new ArrayDeque<>();
    private static final Queue<Supplier<Runnable>> PENDING_MODELS = new ArrayDeque<>();
    private static boolean setupReady;
    private static boolean modelsReady;

    /** Queue ordinary client hooks until FMLClientSetup's main-thread work phase. */
    public static void run(Supplier<Runnable> action) {
        run(FMLEnvironment.dist, action);
    }

    static void run(Dist dist, Supplier<Runnable> action) {
        if (dist != Dist.CLIENT) return;
        boolean executeNow;
        synchronized (LOCK) {
            if (!setupReady) {
                PENDING_SETUP.add(action);
                return;
            }
            executeNow = true;
        }
        if (executeNow) execute(action);
    }

    /** Queue only PartialModel declarations until before Flywheel scans RegisterAdditional. */
    public static void models(Supplier<Runnable> action) {
        models(FMLEnvironment.dist, action);
    }

    static void models(Dist dist, Supplier<Runnable> action) {
        if (dist != Dist.CLIENT) return;
        boolean executeNow;
        synchronized (LOCK) {
            if (!modelsReady) {
                PENDING_MODELS.add(action);
                return;
            }
            executeNow = true;
        }
        if (executeNow) execute(action);
    }

    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        onRegisterAdditional(FMLEnvironment.dist, event);
    }

    static void onRegisterAdditional(Dist dist, ModelEvent.RegisterAdditional event) {
        if (dist == Dist.CLIENT) flushModels();
    }

    /** Runs before Flywheel's RegisterAdditional listener scans PartialModel.ALL. */
    public static void flushModels() {
        synchronized (LOCK) {
            if (modelsReady) return;
            modelsReady = true;
        }
        drain(PENDING_MODELS);
    }

    /** Called from FMLClientSetupEvent.enqueueWork on the client main thread. */
    public static void onClientSetup() {
        onClientSetup(FMLEnvironment.dist);
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) event.enqueueWork(() -> ClientRegistration.onClientSetup());
    }

    static void onClientSetup(Dist dist) {
        if (dist != Dist.CLIENT) return;
        synchronized (LOCK) {
            if (setupReady) return;
            setupReady = true;
        }
        drain(PENDING_SETUP);
    }

    private static void drain(Queue<Supplier<Runnable>> pending) {
        Throwable failure = null;
        for (;;) {
            Supplier<Runnable> action;
            synchronized (LOCK) {
                action = pending.poll();
            }
            if (action == null) break;
            try {
                execute(action);
            } catch (Throwable error) {
                if (failure == null) failure = error;
                else if (failure != error) failure.addSuppressed(error);
            }
        }
        if (failure instanceof RuntimeException runtime) throw runtime;
        if (failure instanceof Error error) throw error;
        if (failure != null) throw new RuntimeException(failure);
    }

    private static void execute(Supplier<Runnable> action) {
        Runnable declaration = action.get();
        if (declaration != null) declaration.run();
    }

    private ClientRegistration() {}
}
