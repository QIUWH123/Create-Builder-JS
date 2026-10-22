package net.quiwh.cbjs.kinetics;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraftforge.registries.ForgeRegistries;
import net.quiwh.cbjs.api.KineticData;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.kubejs.CBJSEvents;
import net.quiwh.cbjs.kubejs.KineticOverloadEventJS;
import net.quiwh.cbjs.kubejs.KineticSpeedChangedEventJS;
import net.quiwh.cbjs.kubejs.KineticStressChangedEventJS;

/** Shared event gates for addon kinetics and native blocks managed through kineticData. */
public final class KineticEventDispatcher {
    public static boolean hasChangeListeners(KineticBlockEntity entity) {
        if (!CBJSEvents.KINETIC_SPEED_CHANGED.hasListeners()
                && !CBJSEvents.KINETIC_STRESS_CHANGED.hasListeners()) return false;
        var block = entity.getBlockState().getBlock();
        if (!(block instanceof MaterialGear) && GearLimitManager.rule(block) == null) return false;
        var id = ForgeRegistries.BLOCKS.getKey(block);
        return CBJSEvents.KINETIC_SPEED_CHANGED.hasListeners(id)
                || CBJSEvents.KINETIC_STRESS_CHANGED.hasListeners(id);
    }

    public static boolean isCurrent(KineticBlockEntity entity) {
        var level = entity.getLevel();
        return level != null && !level.isClientSide && !entity.isRemoved()
                && level.getBlockEntity(entity.getBlockPos()) == entity;
    }

    public static void changed(KineticBlockEntity entity, CogwheelKineticState previous,
                               CogwheelKineticState current) {
        if (!isCurrent(entity)) return;
        var block = entity.getBlockState().getBlock();
        KineticData rule = GearLimitManager.rule(block);
        if (!(block instanceof MaterialGear) && rule == null) return;
        long version = GearLimitManager.rulesVersion();
        var id = ForgeRegistries.BLOCKS.getKey(block);
        // Capture both identities before a speed handler can mutate state or reload listeners.
        KineticStressChangedEventJS stressEvent = current.stressChangedFrom(previous)
                && CBJSEvents.KINETIC_STRESS_CHANGED.hasListeners(id)
                ? new KineticStressChangedEventJS(entity, previous, current) : null;
        if (current.speedChangedFrom(previous) && CBJSEvents.KINETIC_SPEED_CHANGED.hasListeners(id)) {
            CBJSEvents.KINETIC_SPEED_CHANGED.post(new KineticSpeedChangedEventJS(entity, previous, current), id);
        }
        if (stressEvent != null && isCurrent(entity) && entity.getBlockState().getBlock() == block
                && GearLimitManager.rulesVersion() == version && GearLimitManager.rule(block) == rule
                && CBJSEvents.KINETIC_STRESS_CHANGED.hasListeners(id)) {
            CBJSEvents.KINETIC_STRESS_CHANGED.post(stressEvent, id);
        }
    }

    public static boolean hasOverloadListeners(KineticBlockEntity entity) {
        if (!CBJSEvents.KINETIC_OVERLOAD.hasListeners()) return false;
        var block = entity.getBlockState().getBlock();
        return (block instanceof MaterialGear || GearLimitManager.rule(block) != null)
                && CBJSEvents.KINETIC_OVERLOAD.hasListeners(ForgeRegistries.BLOCKS.getKey(block));
    }

    public static boolean overloadCancelled(KineticBlockEntity entity, KineticData data, double stress) {
        if (!hasOverloadListeners(entity)) return false;
        // RPM-only limits avoid network reads until a matching overload listener needs the payload.
        double demand = data.maxNetworkStress() == null ? NetworkStressAccess.get(entity) : stress;
        Integer maxRpm = data.independentRpmLimit() ? data.maxSpeed()
                : data.effectiveMaxRpm(AllConfigs.server().kinetics.maxRotationSpeed.get());
        return CBJSEvents.KINETIC_OVERLOAD.post(new KineticOverloadEventJS(entity,
                entity.getSpeed(), demand, maxRpm, data.maxNetworkStress()),
                ForgeRegistries.BLOCKS.getKey(entity.getBlockState().getBlock())).interruptFalse();
    }

    private KineticEventDispatcher() { }
}
