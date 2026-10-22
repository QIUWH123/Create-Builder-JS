package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.IEventHandler;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import net.quiwh.cbjs.kubejs.FluidPipeFlowFilters;
import net.quiwh.cbjs.kubejs.StorageContentFilters;
import net.quiwh.cbjs.kubejs.EventFilterValidation;
import net.quiwh.cbjs.kubejs.BeltEventFilters;
import net.quiwh.cbjs.belts.BeltEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;

/** Adds pipe and storage selectors while preserving KubeJS's native event lifecycle. */
@Mixin(value = EventHandler.class, remap = false)
public abstract class KubeJSFluidPipeFlowMixin {
    @WrapMethod(method = "call", remap = false)
    private Object cbjs$flowArguments(Context context, Scriptable scope, Scriptable self,
                                         Object[] arguments, Operation<Object> original) {
        EventHandler event = (EventHandler) (Object) this;
        return original.call(context, scope, self,
                EventFilterValidation.arguments(event,
                        BeltEventFilters.arguments(event, StorageContentFilters.arguments(event,
                                FluidPipeFlowFilters.arguments(event, arguments)))));
    }

    @WrapMethod(method = "listen", remap = false)
    private void cbjs$flowListener(ScriptType type, Object extraId, IEventHandler callback,
                                       Operation<Void> original) {
        EventHandler event = (EventHandler) (Object) this;
        var exactExtras = EventFilterValidation.exactExtras(event, extraId);
        if (exactExtras != null) {
            for (Object exact : exactExtras) original.call(type, exact, callback);
            return;
        }
        IEventHandler filtered = FluidPipeFlowFilters.isFlowEvent(event)
                ? FluidPipeFlowFilters.filteredHandler(extraId, callback)
                : BeltEventFilters.handles(event) ? BeltEventFilters.filteredHandler(event, extraId, callback)
                : StorageContentFilters.filteredHandler(event, extraId, callback);
        original.call(type, filtered == null ? extraId : null, filtered == null ? callback : filtered);
        FluidPipeFlowFilters.invalidate(event, type);
        StorageContentFilters.invalidate(event, type);
        BeltEventFilters.invalidate(event, type);
    }

    @WrapMethod(method = "clear", remap = false)
    private void cbjs$clearFlowListeners(ScriptType type, Operation<Void> original) {
        original.call(type);
        FluidPipeFlowFilters.invalidate((EventHandler) (Object) this, type);
        StorageContentFilters.invalidate((EventHandler) (Object) this, type);
        BeltEventFilters.invalidate((EventHandler) (Object) this, type);
        BeltEventDispatcher.invalidate((EventHandler) (Object) this, type);
        net.quiwh.cbjs.storage.StorageEventDispatcher.invalidate((EventHandler) (Object) this, type);
        net.quiwh.cbjs.kubejs.GogglesTooltipDispatcher.invalidate((EventHandler) (Object) this, type);
    }
}
