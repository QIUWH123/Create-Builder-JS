package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.quiwh.cbjs.content.ScriptedBeltConnectorItem;
import net.quiwh.cbjs.content.MechanicalBeltShafts;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.quiwh.cbjs.content.MechanicalBeltLengths;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeltConnectorItem.class, remap = false)
public abstract class MechanicalBeltConnectorMixin {
    @WrapOperation(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;canConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Z"),
            remap = false, require = 1, allow = 1)
    private boolean cbjs$checkSelectedLength(Level level, BlockPos first, BlockPos second,
            Operation<Boolean> original) {
        return MechanicalBeltLengths.withBelt(MechanicalBeltLengths.material((net.minecraft.world.item.Item)(Object)this),
                () -> original.call(level, first, second));
    }

    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;maxLength()Ljava/lang/Integer;"),
            remap = false, require = 1, allow = 1)
    private Integer cbjs$selectionRange() {
        Integer cap = MechanicalBeltLengths.customConfigured(
                MechanicalBeltLengths.material((net.minecraft.world.item.Item)(Object)this));
        return cap == null ? BeltConnectorItem.maxLength() : cap;
    }

    @Redirect(method = "canConnect", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;maxLength()Ljava/lang/Integer;"),
            remap = false, require = 1, allow = 1)
    private static Integer cbjs$connectionRange() {
        Integer cap = MechanicalBeltLengths.current();
        return cap == null ? BeltConnectorItem.maxLength() : cap;
    }

    @Group(name = "cbjs$connectionDistance", min = 1, max = 1)
    @WrapOperation(method = "canConnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$connectionDistance(BlockPos second, Vec3i first, double nativeCap,
            Operation<Boolean> original) {
        Integer cap = MechanicalBeltLengths.current();
        return cap == null ? original.call(second, first, nativeCap) : MechanicalBeltLengths.within(first, second, cap);
    }

    @Group(name = "cbjs$connectionDistance", min = 1, max = 1)
    @WrapOperation(method = "canConnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;m_123314_(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private static boolean cbjs$connectionDistanceSrg(BlockPos second, Vec3i first, double nativeCap,
            Operation<Boolean> original) {
        return cbjs$connectionDistance(second, first, nativeCap, original);
    }

    @Group(name = "cbjs$selectionDistance", min = 1, max = 1)
    @WrapOperation(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private boolean cbjs$selectionDistance(BlockPos first, Vec3i second, double nativeCap,
            Operation<Boolean> original) {
        Integer cap = MechanicalBeltLengths.customConfigured(
                MechanicalBeltLengths.material((net.minecraft.world.item.Item)(Object)this));
        return cap == null ? original.call(first, second, nativeCap) : MechanicalBeltLengths.within(first, second, (long) cap * 2);
    }

    @Group(name = "cbjs$selectionDistance", min = 1, max = 1)
    @WrapOperation(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/BlockPos;m_123314_(Lnet/minecraft/core/Vec3i;D)Z"),
            remap = false, require = 0, allow = 1)
    private boolean cbjs$selectionDistanceSrg(BlockPos first, Vec3i second, double nativeCap,
            Operation<Boolean> original) {
        return cbjs$selectionDistance(first, second, nativeCap, original);
    }

    @Group(name = "cbjs$loadedPath", min = 1, max = 1)
    @Inject(method = "canConnect", at = @At(value = "INVOKE", ordinal = 2,
            target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            cancellable = true, remap = false, require = 0, allow = 1)
    private static void cbjs$requireLoadedPath(Level level, BlockPos first, BlockPos second,
            CallbackInfoReturnable<Boolean> callback, @Local(name = "currentPos") BlockPos current) {
        if (MechanicalBeltLengths.hasSelectedBelt() && !level.isLoaded(current)) callback.setReturnValue(false);
    }

    @Group(name = "cbjs$loadedPath", min = 1, max = 1)
    @Inject(method = "canConnect", at = @At(value = "INVOKE", ordinal = 2,
            target = "Lnet/minecraft/world/level/Level;m_8055_(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"),
            cancellable = true, remap = false, require = 0, allow = 1)
    private static void cbjs$requireLoadedPathSrg(Level level, BlockPos first, BlockPos second,
            CallbackInfoReturnable<Boolean> callback, @Local(name = "currentPos") BlockPos current) {
        cbjs$requireLoadedPath(level, first, second, callback, current);
    }

    @ModifyConstant(method = "canConnect", constant = @Constant(intValue = 1000), remap = false, require = 1, allow = 1)
    private static int cbjs$validateFullPath(int original, Level level, BlockPos first, BlockPos second) {
        return MechanicalBeltLengths.hasSelectedBelt() ? MechanicalBeltLengths.endpointBudget(first, second) : original;
    }

    @ModifyConstant(method = "getBeltChainBetween", constant = @Constant(intValue = 1000), remap = false, require = 1, allow = 1)
    private static int cbjs$buildFullPath(int original, BlockPos first, BlockPos second,
            com.simibubi.create.content.kinetics.belt.BeltSlope slope, net.minecraft.core.Direction direction) {
        return MechanicalBeltLengths.configured(MechanicalBeltCompat.placementState(null)) == null
                ? original : MechanicalBeltLengths.endpointBudget(first, second);
    }

    @Inject(method = "createBelts", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$limitDirectCreation(Level level, BlockPos first, BlockPos second,
            CallbackInfo callback) {
        if (!MechanicalBeltLengths.allowsConnection(MechanicalBeltCompat.placementState(null), first, second))
            callback.cancel();
    }

    @Redirect(method = {"validateAxis", "canConnect", "createBelts"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/simpleRelays/ShaftBlock;isShaft(Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            remap = false, require = 3, allow = 3)
    private static boolean cbjs$recognizeCustomShaft(BlockState state) {
        return MechanicalBeltCompat.isShaft(state);
    }

    @WrapOperation(method = "createBelts", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;switchToBlockState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"),
            remap = false, require = 1, allow = 1)
    private static void cbjs$rememberShaft(Level level, BlockPos pos, BlockState belt,
            Operation<Void> original, @Local(ordinal = 2) BlockState shaft) {
        original.call(level, pos, belt);
        if (shaft.getBlock() instanceof net.quiwh.cbjs.content.ScriptedShaftBlock)
            MechanicalBeltShafts.set(level.getBlockEntity(pos), shaft);
    }

    @Redirect(method = {"useOn", "m_6225_"}, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;createBelts(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"),
            remap = false, require = 1, allow = 1)
    private void cbjs$placeSelectedMaterial(Level level, BlockPos start, BlockPos end) {
        if ((Object)this instanceof ScriptedBeltConnectorItem connector)
            MechanicalBeltCompat.createBelts(level, start, end, connector.getBlock().defaultBlockState());
        else BeltConnectorItem.createBelts(level, start, end);
    }

    @Redirect(method = "createBelts", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false, require = 1, allow = 1)
    private static BlockState cbjs$materialState(BlockEntry<?> entry) {
        return MechanicalBeltCompat.placementState(entry.getDefaultState());
    }

    @Redirect(method = "createBelts", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1, allow = 1)
    private static boolean cbjs$cleanUpCustomPlacement(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state)
                && MechanicalBeltCompat.sameFamily(state, MechanicalBeltCompat.placementState(entry.getDefaultState()));
    }
}
