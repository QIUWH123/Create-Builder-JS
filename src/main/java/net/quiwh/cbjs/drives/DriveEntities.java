package net.quiwh.cbjs.drives;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DriveEntities {
    private static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "cbjs");
    public static final DeferredHolder<EntityType<?>, EntityType<DriveRideEntity>> RIDER = TYPES.register("drive_rider",
            () -> EntityType.Builder.<DriveRideEntity>of(DriveRideEntity::new, MobCategory.MISC)
                    .sized(.1f, .1f).noSave().noSummon().clientTrackingRange(8).updateInterval(1).build("cbjs:drive_rider"));
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private DriveEntities() {}
}
