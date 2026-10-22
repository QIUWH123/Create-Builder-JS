package net.quiwh.cbjs.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.quiwh.cbjs.content.ScriptedMechanicalBeltBlock;
import net.quiwh.cbjs.content.MechanicalBeltShaftEdits;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.quiwh.cbjs.content.MechanicalBeltLengths;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;

@Mixin(value = BeltSlicer.class, remap = false)
public abstract class MechanicalBeltSlicerMixin {
    @WrapMethod(method = "useWrench", remap = false)
    private static InteractionResult cbjs$moveShortenedPulley(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit, BeltSlicer.Feedback feedback,
            Operation<InteractionResult> original) {
        return MechanicalBeltShaftEdits.edit(false, state, level, pos, player, hit,
                () -> original.call(state, level, pos, player, hand, hit, feedback));
    }

    @WrapMethod(method = "useConnector", remap = false)
    private static InteractionResult cbjs$moveExtendedPulley(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit, BeltSlicer.Feedback feedback,
            Operation<InteractionResult> original) {
        Integer cap = MechanicalBeltLengths.customConfigured(state);
        if (cap != null) {
            var controller = BeltHelper.getControllerBE(level, pos);
            if (controller != null && controller.beltLength >= cap) return InteractionResult.FAIL;
        }
        return MechanicalBeltShaftEdits.edit(true, state, level, pos, player, hit,
                () -> original.call(state, level, pos, player, hand, hit, feedback));
    }

    @Redirect(method = "useConnector", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/belt/item/BeltConnectorItem;maxLength()Ljava/lang/Integer;"),
            remap = false, require = 2, allow = 2)
    private static Integer cbjs$extensionRange(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, BeltSlicer.Feedback feedback) {
        Integer cap = MechanicalBeltLengths.customConfigured(state);
        return cap == null ? BeltConnectorItem.maxLength() : cap;
    }

    @Redirect(method = "useWrench", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"),
            remap = false, require = 1, allow = 1)
    private static ItemStack cbjs$refundShortenedPulley(BlockEntry<?> entry) {
        return MechanicalBeltShaftEdits.shorteningRefund(entry);
    }

    @Redirect(method = "useConnector", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack(I)Lnet/minecraft/world/item/ItemStack;"),
            remap = false, require = 1, allow = 1)
    private static ItemStack cbjs$refundMergedPulleys(BlockEntry<?> entry, int amount) {
        return MechanicalBeltShaftEdits.mergingRefund(entry, amount);
    }

    @Redirect(method = {"useWrench", "useConnector"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 4, allow = 4)
    private static boolean cbjs$editCustomBelts(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    @Redirect(method = "useWrench", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false, require = 1, allow = 1)
    private static boolean cbjs$splitWithMatchingConnector(ItemEntry<?> entry, ItemStack item,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit, BeltSlicer.Feedback feedback) {
        return MechanicalBeltCompat.recognizeConnector(entry, item, state);
    }

    @Redirect(method = {"useWrench", "useConnector"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;asStack()Lnet/minecraft/world/item/ItemStack;"),
            remap = false, require = 2, allow = 2)
    private static ItemStack cbjs$refundMatchingConnector(ItemEntry<?> entry, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, BeltSlicer.Feedback feedback) {
        return entry == AllItems.BELT_CONNECTOR ? MechanicalBeltCompat.connector(state) : entry.asStack();
    }

    @Inject(method = "beltStatesCompatible", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$rejectMixedMaterials(BlockState state, BlockState next,
            CallbackInfoReturnable<Boolean> callback) {
        if ((state.getBlock() instanceof ScriptedMechanicalBeltBlock || next.getBlock() instanceof ScriptedMechanicalBeltBlock)
                && !MechanicalBeltCompat.sameFamily(state, next)) callback.setReturnValue(false);
    }
}
