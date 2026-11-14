package net.quiwh.cbjs.content;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.quiwh.cbjs.client.NativeGeneratorClientModels;

/** Keep capacity keyed to this crank rather than Create's valve-handle fallback. */
public final class ScriptedHandCrankBlockEntity extends HandCrankBlockEntity {
    public ScriptedHandCrankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    @Override protected Block getStressConfigKey() { return getBlockState().getBlock(); }
    @OnlyIn(Dist.CLIENT)
    @Override public SuperByteBuffer getRenderedHandle() {
        return CachedBuffers.partialFacing(NativeGeneratorClientModels.part(getBlockState(), "handle", com.simibubi.create.AllPartialModels.HAND_CRANK_HANDLE),
                getBlockState(), getBlockState().getValue(com.simibubi.create.content.kinetics.crank.HandCrankBlock.FACING).getOpposite());
    }
    @OnlyIn(Dist.CLIENT)
    @Override public void tickAudio() {
        super.tickAudio();
        if (inUse > 0 && AnimationTickHolder.getTicks() % 10 == 0)
            AllSoundEvents.CRANKING.playAt(level, worldPosition, inUse / 2.5f, .65f + (10 - inUse) / 10f, true);
    }
}
