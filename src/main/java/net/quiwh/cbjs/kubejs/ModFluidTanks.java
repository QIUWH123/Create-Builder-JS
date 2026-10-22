package net.quiwh.cbjs.kubejs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.quiwh.cbjs.client.FluidTankClientModels;
import net.quiwh.cbjs.content.ScriptedFluidTankBlockEntity;

/** A distinct native multiblock family per script ID prevents different materials merging. */
public final class ModFluidTanks {
    private static final Map<ResourceLocation, FluidTankBuilder> BUILDERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, BlockEntityType<ScriptedFluidTankBlockEntity>> TYPES = new LinkedHashMap<>();

    static void add(FluidTankBuilder builder) { BUILDERS.put(builder.id, builder); }

    static java.util.List<FluidTankBuilder> builders() { return java.util.List.copyOf(BUILDERS.values()); }

    public static BlockEntityType<ScriptedFluidTankBlockEntity> type(ResourceLocation blockId) {
        var type = TYPES.get(blockId);
        if (type == null) throw new IllegalStateException("Fluid tank block entity type is not registered: " + blockId);
        return type;
    }

    public static void forEach(BiConsumer<FluidTankBuilder, BlockEntityType<ScriptedFluidTankBlockEntity>> consumer) {
        BUILDERS.forEach((id, builder) -> consumer.accept(builder, type(id)));
    }

    public static void resolveServerSettings(ServerAboutToStartEvent event) {
        for (var builder : BUILDERS.values()) builder.resolveServerSettings();
    }

    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var builder : BUILDERS.values()) {
            var type = BlockEntityType.Builder.of((pos, state) ->
                    builder.isVessel() ? new net.quiwh.cbjs.content.ScriptedFluidVesselBlockEntity(type(builder.id), pos, state)
                            : new ScriptedFluidTankBlockEntity(type(builder.id), pos, state), builder.get()).build(null);
            TYPES.put(builder.id, type);
            ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath("cbjs",
                    (builder.isVessel() ? "fluid_vessel/" : "fluid_tank/") + builder.id.getNamespace() + "/" + builder.id.getPath());
            event.register(Registries.BLOCK_ENTITY_TYPE, typeId, () -> type);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> FluidTankClientModels.register(builder));
        }
    }

    public static void registerMovement() {
        for (var builder : BUILDERS.values()) {
            net.quiwh.cbjs.content.ModFluidTankMountedStorage.associate(builder.get());
            com.simibubi.create.api.behaviour.movement.MovementBehaviour.REGISTRY.register(builder.get(),
                    new com.simibubi.create.content.fluids.tank.FluidTankMovementBehavior());
        }
    }

    private ModFluidTanks() { }
}
