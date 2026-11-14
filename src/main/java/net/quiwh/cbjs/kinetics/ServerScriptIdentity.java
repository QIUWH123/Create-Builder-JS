package net.quiwh.cbjs.kinetics;

import dev.latvian.mods.kubejs.core.ReloadableServerResourcesKJS;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Public KubeJS reload-generation identity; replaced when server scripts reload. */
public final class ServerScriptIdentity {
    public static Object current() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        var resources = server.getServerResources();
        var manager = ((ReloadableServerResourcesKJS) (Object) resources.managers()).kjs$getServerScriptManager();
        return manager == null ? null : manager.contextFactory;
    }
    private ServerScriptIdentity() { }
}
