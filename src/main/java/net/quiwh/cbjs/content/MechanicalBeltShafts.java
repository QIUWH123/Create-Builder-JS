package net.quiwh.cbjs.content;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Original custom pulley material, stored per segment in Forge's saved block entity data. */
public final class MechanicalBeltShafts {
    public static final String TAG = "CBJSShaft";
    private static final String LEGACY_TAG = "KineticJSShaft";

    public static BlockState get(BlockEntity entity) {
        if (entity == null) return null;
        var persistent = entity.getPersistentData();
        String key = persistent.contains(TAG) ? TAG : LEGACY_TAG;
        if (!persistent.contains(key, Tag.TAG_COMPOUND)) return null;
        var data = persistent.getCompound(key);
        return entity instanceof MechanicalBeltShaftAccess cached ? cached.cbjs$shaftMaterial(data) : decode(data);
    }

    public static BlockState decode(CompoundTag data) {
        ResourceLocation id = ResourceLocation.tryParse(data.getString("Name"));
        if (id == null || !(BuiltInRegistries.BLOCK.get(id) instanceof ScriptedShaftBlock)) return null;
        BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), data);
        return state.getBlock() instanceof ScriptedShaftBlock ? state : null;
    }

    public static void set(BlockEntity entity, BlockState shaft) {
        if (entity == null) return;
        var persistent = entity.getPersistentData();
        boolean changed = persistent.contains(LEGACY_TAG);
        persistent.remove(LEGACY_TAG);
        if (shaft != null && shaft.getBlock() instanceof ScriptedShaftBlock) {
            CompoundTag data = NbtUtils.writeBlockState(shaft);
            if (!data.equals(persistent.get(TAG))) {
                persistent.put(TAG, data);
                changed = true;
            }
        }
        else if (persistent.contains(TAG)) {
            persistent.remove(TAG);
            changed = true;
        }
        if (!changed) return;
        entity.setChanged();
        if (entity instanceof SyncedBlockEntity synced) synced.sendData();
    }

    public static BlockState shaftState(BlockEntity entity, BlockState fallback) {
        BlockState shaft = get(entity);
        return shaft == null ? fallback : shaft;
    }

    public static ItemStack shaftItem(BlockState shaft) {
        return shaft != null && shaft.getBlock() instanceof ScriptedShaftBlock
                ? new ItemStack(shaft.getBlock()) : AllBlocks.SHAFT.asStack();
    }

    private MechanicalBeltShafts() { }
}
