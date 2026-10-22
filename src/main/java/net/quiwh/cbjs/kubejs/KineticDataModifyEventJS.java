package net.quiwh.cbjs.kubejs;

import dev.latvian.mods.kubejs.event.EventJS;
import net.quiwh.cbjs.api.event.KineticDataModifyEvent;

/** Reloadable overrides for builder defaults and other Create kinetic blocks. */
public final class KineticDataModifyEventJS extends EventJS {
    private final KineticDataModifyEvent event;

    public KineticDataModifyEventJS(KineticDataModifyEvent event) {
        this.event = event;
    }

    public KineticDataModifyEvent.KineticDataModifier modifyKineticData(String blockId) {
        return event.modifyKineticData(blockId);
    }
}
