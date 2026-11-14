package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.BoilerEngineCapacity;
import net.quiwh.cbjs.content.BoilerEngineCapacityAccess;
import net.quiwh.cbjs.content.SteamFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = com.simibubi.create.content.fluids.tank.BoilerData.class, remap = false)
public abstract class SteamBoilerIdentityMixin implements BoilerEngineCapacityAccess {
    @Shadow public int attachedEngines;
    @Unique private final BoilerEngineCapacity cbjs$engineCapacity = new BoilerEngineCapacity();

    @Override public BoilerEngineCapacity cbjs$engineCapacity() { return cbjs$engineCapacity; }

    @Redirect(method = {"evaluate", "checkPipeOrganAdvancement"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private boolean cbjs$steamFamily(BlockEntry<?> entry, BlockState state) { return SteamFamilies.has(entry, state); }

    @Inject(method = "evaluate", at = @At("HEAD"))
    private void cbjs$beginEngineTypes(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> ci) {
        cbjs$engineCapacity.begin();
    }

    @WrapOperation(method = "evaluate", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/steamEngine/SteamEngineBlock;getFacing(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/core/Direction;"),
            require = 1, allow = 1)
    private Direction cbjs$recordEngineType(BlockState state, Operation<Direction> original, @Local Direction side) {
        Direction facing = original.call(state);
        if (facing == side) cbjs$engineCapacity.add(state.getBlock());
        return facing;
    }

    @Inject(method = "evaluate", at = @At("RETURN"), cancellable = true)
    private void cbjs$finishEngineTypes(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> ci) {
        if (cbjs$engineCapacity.finish(attachedEngines)) ci.setReturnValue(true);
    }

    @WrapOperation(method = "addToGoggleTooltip", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/api/stress/BlockStressValues;getCapacity(Lnet/minecraft/world/level/block/Block;)D"),
            require = 1, allow = 1)
    private double cbjs$engineCapacityForTooltip(Block block, Operation<Double> original) {
        return cbjs$engineCapacity.average(original.call(block), attachedEngines);
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void cbjs$writeEngineTypes(CallbackInfoReturnable<CompoundTag> ci) {
        cbjs$engineCapacity.write(ci.getReturnValue());
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void cbjs$readEngineTypes(CompoundTag tag, int boilerSize, CallbackInfo ci) {
        cbjs$engineCapacity.read(tag, attachedEngines);
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void cbjs$clearEngineTypes(CallbackInfo ci) { cbjs$engineCapacity.clear(); }

    @Inject(method = "tick", at = @At("HEAD"))
    private void cbjs$recoverLegacyEngineTypes(FluidTankBlockEntity controller, CallbackInfo ci) {
        if (!cbjs$engineCapacity.needsRefresh()) return;
        var level = controller.getLevel();
        if (level == null || level.isClientSide) return;
        Direction.Axis axis = controller.getMainConnectionAxis();
        int width = controller.getWidth(), length = controller.getHeight();
        var origin = controller.getBlockPos();
        var end = origin.offset(axis == Direction.Axis.X ? length : width,
                axis == Direction.Axis.Y ? length : width, axis == Direction.Axis.Z ? length : width);
        // A legacy save needs one normal topology refresh. Never load missing chunks for a tooltip.
        if (level.hasChunksAt(origin.offset(-1, -1, -1), end)) controller.updateBoilerState();
    }
}
