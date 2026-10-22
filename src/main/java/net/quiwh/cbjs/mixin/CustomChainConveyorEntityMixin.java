package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.quiwh.cbjs.compat.createchain.ConveyorChainAccess;
import net.quiwh.cbjs.compat.createchain.ConveyorChainData;
import net.quiwh.cbjs.compat.createchain.ConveyorChains;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Saves material per conveyor edge and uses it for destruction and particle effects. */
@Mixin(value = ChainConveyorBlockEntity.class, remap = false)
public abstract class CustomChainConveyorEntityMixin implements ConveyorChainAccess {
    @Shadow private BlockPos chainDestroyedEffectToSend;
    @Unique private ConveyorChainData cbjs$chainMaterials;
    @Unique private ResourceLocation cbjs$destroyedMaterial;

    @Override public ConveyorChainData cbjs$chains() {
        if (cbjs$chainMaterials == null) cbjs$chainMaterials = new ConveyorChainData();
        return cbjs$chainMaterials;
    }

    @Override public ConveyorChainData cbjs$existingChains() { return cbjs$chainMaterials; }

    @Inject(method = "tick", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;tick()V", shift = At.Shift.AFTER), remap = false)
    private void cbjs$checkChainLimits(CallbackInfo ci) {
        ConveyorChains.checkLimits((ChainConveyorBlockEntity)(Object)this);
    }

    @Inject(method = "writeSafe", at = @At("HEAD"), remap = false)
    private void cbjs$saveMaterials(CompoundTag tag, CallbackInfo ci) {
        if (cbjs$chainMaterials != null)
            cbjs$chainMaterials.write(tag, ((ChainConveyorBlockEntity)(Object)this).connections);
    }

    @Inject(method = "write", at = @At("HEAD"), remap = false)
    private void cbjs$saveDestroyMaterial(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (cbjs$chainMaterials != null)
            cbjs$chainMaterials.write(tag, ((ChainConveyorBlockEntity)(Object)this).connections);
        if (clientPacket && chainDestroyedEffectToSend != null) {
            ResourceLocation material = cbjs$destroyedMaterial == null
                    ? ConveyorChains.material((ChainConveyorBlockEntity)(Object)this, chainDestroyedEffectToSend)
                    : cbjs$destroyedMaterial;
            if (!ChainMaterials.VANILLA.equals(material)) tag.putString("CBJSChainDestroyedMaterial", material.toString());
        }
    }

    @Inject(method = "read", at = @At("HEAD"), remap = false)
    private void cbjs$loadMaterials(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (ConveyorChainData.hasSavedMaterials(tag) || cbjs$chainMaterials != null) cbjs$chains().read(tag);
        cbjs$destroyedMaterial = ChainMaterials.normalize(
                ResourceLocation.tryParse(tag.getString("CBJSChainDestroyedMaterial")));
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false)
    private void cbjs$pruneMaterials(CompoundTag tag, boolean clientPacket, CallbackInfo ci) {
        if (cbjs$chainMaterials != null)
            cbjs$chainMaterials.retain(((ChainConveyorBlockEntity)(Object)this).connections);
        cbjs$destroyedMaterial = null;
    }

    @Inject(method = "transform", at = @At("HEAD"), remap = false)
    private void cbjs$transformMaterials(BlockEntity entity, StructureTransform transform, CallbackInfo ci) {
        if (cbjs$chainMaterials != null) cbjs$chainMaterials.transform(transform::applyWithoutOffset);
    }

    @WrapMethod(method = "removeConnectionTo", remap = false)
    private boolean cbjs$removeMaterial(BlockPos target, Operation<Boolean> original) {
        ChainConveyorBlockEntity entity = (ChainConveyorBlockEntity)(Object)this;
        if (cbjs$chainMaterials == null) return original.call(target);
        BlockPos relative = target.subtract(entity.getBlockPos());
        ResourceLocation material = cbjs$chains().material(relative);
        cbjs$chains().remove(relative);
        boolean removed = original.call(target);
        if (!removed) cbjs$chains().put(relative, material);
        return removed;
    }

    @Inject(method = "chainDestroyed", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$dropMaterial(BlockPos target, boolean spawnDrops, boolean sendEffect, CallbackInfo ci) {
        ResourceLocation material = ConveyorChains.material((ChainConveyorBlockEntity)(Object)this, target);
        if (ChainMaterials.VANILLA.equals(material)) return;
        ChainConveyorBlockEntity entity = (ChainConveyorBlockEntity)(Object)this;
        var level = entity.getLevel();
        if (level == null) return;
        if (sendEffect) {
            chainDestroyedEffectToSend = target;
            cbjs$destroyedMaterial = material;
            entity.sendData();
            cbjs$destroyedMaterial = null;
        }
        if (spawnDrops) {
            int amount = ChainConveyorBlockEntity.getChainCost(target);
            if (!entity.forPointsAlongChains(target, amount,
                    point -> level.addFreshEntity(new ItemEntity(level, point.x, point.y, point.z,
                            ChainMaterials.stack(material, 1))))) {
                while (amount > 0) {
                    var stack = ChainMaterials.stack(material, 1);
                    int count = Math.min(amount, stack.getMaxStackSize());
                    stack.setCount(count);
                    Block.popResource(level, entity.getBlockPos(), stack);
                    amount -= count;
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "spawnDestroyParticles", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$materialParticles(BlockPos target, CallbackInfo ci) {
        ResourceLocation material = cbjs$destroyedMaterial == null
                ? ConveyorChains.material((ChainConveyorBlockEntity)(Object)this, target) : cbjs$destroyedMaterial;
        if (ChainMaterials.VANILLA.equals(material)) return;
        ChainConveyorBlockEntity entity = (ChainConveyorBlockEntity)(Object)this;
        if (entity.getLevel() == null) return;
        entity.forPointsAlongChains(target, (int)Math.round(Vec3.atLowerCornerOf(target).length() * 8),
                point -> entity.getLevel().addParticle(new BlockParticleOption(ParticleTypes.BLOCK,
                        ChainMaterials.blockState(material)), point.x, point.y, point.z, 0, 0, 0));
        ci.cancel();
    }
}
