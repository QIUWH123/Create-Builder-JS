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
import net.quiwh.cbjs.client.ItemVaultClientModels;
import net.quiwh.cbjs.content.ScriptedItemVaultBlockEntity;
import net.quiwh.cbjs.content.ModItemVaultMountedStorage;

/** Each script ID has its own multiblock type and cannot join another material. */
public final class ModItemVaults {
    private static final Map<ResourceLocation, ItemVaultBuilder> BUILDERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, BlockEntityType<ScriptedItemVaultBlockEntity>> TYPES = new LinkedHashMap<>();
    static void add(ItemVaultBuilder builder) { BUILDERS.put(builder.id, builder); }
    static java.util.List<ItemVaultBuilder> builders() { return java.util.List.copyOf(BUILDERS.values()); }
    public static BlockEntityType<ScriptedItemVaultBlockEntity> type(ResourceLocation id) {
        var type = TYPES.get(id);
        if (type == null) throw new IllegalStateException("Item storage entity type is not registered: " + id);
        return type;
    }
    public static void forEach(BiConsumer<ItemVaultBuilder, BlockEntityType<ScriptedItemVaultBlockEntity>> consumer) {
        BUILDERS.forEach((id, builder) -> consumer.accept(builder, type(id)));
    }
    public static void resolveServerSettings(ServerAboutToStartEvent event) {
        for (var builder : BUILDERS.values()) builder.resolveServerSettings();
    }
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) return;
        for (var builder : BUILDERS.values()) {
            var type = BlockEntityType.Builder.of((pos, state) ->
                    new ScriptedItemVaultBlockEntity(type(builder.id), pos, state), builder.get()).build(null);
            TYPES.put(builder.id, type);
            event.register(Registries.BLOCK_ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("cbjs",
                    "item_storage/" + builder.id.getNamespace() + "/" + builder.id.getPath()), () -> type);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ItemVaultClientModels.register(builder));
        }
    }
    public static void registerMovement() {
        for (var builder : BUILDERS.values()) ModItemVaultMountedStorage.associate(builder.get());
    }
    private ModItemVaults() { }
}
