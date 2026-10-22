package net.quiwh.cbjs.compat.bnb;

import com.kipti.bnb.content.cogwheel_chain.block.CogwheelChainBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Stores the exact source state across chaining, reloads and schematic copying. */
public final class BnbChainedCogwheelBlockEntity extends CogwheelChainBlockEntity {
    private static final String SOURCE_STATE = "CBJSSourceState";
    private static final String LEGACY_SOURCE_STATE = "KineticJSSourceState";
    private BlockState sourceState;

    public BnbChainedCogwheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setSourceState(BlockState state) {
        var block = (BnbChainedCogwheelBlock) getBlockState().getBlock();
        if (state.getBlock() != block.sourceBlock())
            throw new IllegalArgumentException("Chained cogwheel source does not match its companion");
        sourceState = state;
        setChanged();
    }

    public BlockState sourceState() {
        var block = (BnbChainedCogwheelBlock) getBlockState().getBlock();
        BlockState saved = sourceState == null ? block.sourceState(getBlockState()) : sourceState;
        // Wrench/contraption transformations may change the companion's axis or custom states.
        for (var property : getBlockState().getProperties())
            if (saved.hasProperty(property)) saved = copyProperty(saved, getBlockState(), property);
        return saved;
    }

    private static <T extends Comparable<T>> BlockState copyProperty(BlockState target, BlockState from,
                                                                     Property<T> property) {
        return target.setValue(property, from.getValue(property));
    }

    @Override protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        sourceState = null;
        String key = tag.contains(SOURCE_STATE) ? SOURCE_STATE : LEGACY_SOURCE_STATE;
        if (tag.contains(key, Tag.TAG_COMPOUND)) {
            var state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(), tag.getCompound(key));
            var block = (BnbChainedCogwheelBlock) getBlockState().getBlock();
            if (state.getBlock() == block.sourceBlock()) sourceState = state;
        }
    }

    @Override protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        writeSourceState(tag);
    }

    @Override public void writeSafe(CompoundTag tag) {
        super.writeSafe(tag);
        writeSourceState(tag);
    }

    private void writeSourceState(CompoundTag tag) {
        tag.remove(LEGACY_SOURCE_STATE);
        tag.put(SOURCE_STATE, NbtUtils.writeBlockState(sourceState()));
    }
}
