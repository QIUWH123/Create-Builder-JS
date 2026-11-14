package net.quiwh.cbjs.kinetics;

import java.util.Collection;
import net.neoforged.fml.ModList;

/** Detects load errors and swallowed callback errors in the pinned KubeJS 2101.7.2 runtime. */
public final class ScriptReloadGuard {
    public static void requireSuccessfulServerScripts() {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        try {
            Class<?> consoleClass = Class.forName("dev.latvian.mods.kubejs.script.ConsoleJS");
            Object console = consoleClass.getField("SERVER").get(null);
            Object errors = consoleClass.getField("errors").get(console);
            if (!(errors instanceof Collection<?> messages)) {
                throw new IllegalStateException("Unsupported KubeJS server error collection");
            }
            if (!messages.isEmpty()) {
                throw new IllegalStateException("KubeJS reported " + messages.size()
                        + " server script error(s); see logs/kubejs/server.log, fix the script and reload again");
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot validate this KubeJS version; keeping previous limits", exception);
        }
    }

    private ScriptReloadGuard() {
    }
}
