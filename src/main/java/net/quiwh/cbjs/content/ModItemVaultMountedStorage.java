package net.quiwh.cbjs.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.api.contraption.storage.item.WrapperMountedItemStorage;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.foundation.utility.CreateCodecs;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraftforge.items.ItemStackHandler;
import net.quiwh.cbjs.CBJS;
import org.jetbrains.annotations.Nullable;

/** Per-block mounted inventories preserve independent materials and drainable saved overflow. */
public final class ModItemVaultMountedStorage {
    public static final RegistryEntry<Type> TYPE = CBJS.REGISTRATE.mountedItemStorage("scripted_item_storage", Type::new).register();
    public static void register() { }
    public static void associate(Block block) {
        if (!(block instanceof ScriptedItemVaultBlock)) return;
        MountedItemStorageType.REGISTRY.register(block, TYPE.get());
        InventoryIdentifier.REGISTRY.register(block, (level, state, face) ->
                level.getBlockEntity(face.getPos()) instanceof ScriptedItemVaultBlockEntity vault ? vault.getInvId() : null);
    }
    public static final class Type extends MountedItemStorageType<Storage> {
        public Type() { super(Storage.CODEC); }
        @Override public @Nullable Storage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
            return be instanceof ScriptedItemVaultBlockEntity vault ? Storage.fromVault(vault) : null;
        }
    }
    public static final class Storage extends WrapperMountedItemStorage<ItemStackHandler> {
        public static final Codec<Storage> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("block").forGetter(storage -> storage.blockId),
                Codec.INT.fieldOf("slots").forGetter(Storage::currentSlots),
                CreateCodecs.ITEM_STACK_HANDLER.fieldOf("inventory").forGetter(Storage::inventoryForSave)
        ).apply(i, Storage::new));
        private final ResourceLocation blockId;
        private Storage(ResourceLocation blockId, int allowedSlots, ItemStackHandler source) {
            super(TYPE.get(), new Handler(blockId, allowedSlots, source.getSlots()));
            this.blockId = blockId;
            wrapped.deserializeNBT(source.serializeNBT());
            wrapped.getSlots();
        }
        private int currentSlots() { return ((Handler)wrapped).currentSlots(); }
        private ItemStackHandler inventoryForSave() { wrapped.getSlots(); return wrapped; }
        public static Storage fromVault(ScriptedItemVaultBlockEntity vault) {
            return new Storage(BuiltInRegistries.BLOCK.getKey(vault.getBlockState().getBlock()),
                    vault.builder().capacityPerBlock(), copyToItemStackHandler(vault.getInventoryOfBlock()));
        }
        @Override public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
            if (be instanceof ScriptedItemVaultBlockEntity vault
                    && blockId.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()))) vault.applyInventoryToBlock(wrapped);
        }
        @Override public boolean handleInteraction(ServerPlayer player, Contraption contraption, StructureBlockInfo info) { return false; }
    }

    /** Grow on every native access, retaining saved overflow and the material's live intake policy. */
    private static final class Handler extends ItemStackHandler {
        private final ResourceLocation blockId;
        private final int savedSlots;
        private Handler(ResourceLocation blockId, int savedSlots, int size) {
            super(size);
            this.blockId = blockId;
            this.savedSlots = savedSlots;
        }
        private int currentSlots() {
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            return block instanceof ScriptedItemVaultBlock vault ? vault.sourceBuilder().capacityPerBlock() : savedSlots;
        }
        @Override public int getSlots() {
            int current = currentSlots();
            if (current > stacks.size()) {
                var grown = net.minecraft.core.NonNullList.withSize(current, ItemStack.EMPTY);
                for (int slot = 0; slot < stacks.size(); slot++) grown.set(slot, stacks.get(slot));
                stacks = grown;
                MountedItemStorageCapacity.localSlotsChanged();
            }
            return super.getSlots();
        }
        @Override protected void validateSlotIndex(int slot) {
            getSlots();
            super.validateSlotIndex(slot);
        }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            return slot < currentSlots() && (!(block instanceof ScriptedItemVaultBlock vault)
                    || vault.sourceBuilder().acceptsItem(stack));
        }
    }
    private ModItemVaultMountedStorage() { }
}
