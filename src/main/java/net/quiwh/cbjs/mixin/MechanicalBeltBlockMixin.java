package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.MechanicalBeltCompat;
import net.quiwh.cbjs.content.MechanicalBeltShafts;
import net.quiwh.cbjs.content.ScriptedShaftBlock;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import net.quiwh.cbjs.content.MechanicalBeltLengths;
import net.quiwh.cbjs.content.ScriptedMechanicalBeltBlock;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.simibubi.create.content.kinetics.belt.BeltBlock", remap = false)
public abstract class MechanicalBeltBlockMixin {
    @Redirect(method = {"getDrops", "m_49635_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false, require = 1, allow = 1)
    private BlockState cbjs$dropOriginalShaft(BlockEntry<?> entry, BlockState state, LootParams.Builder loot) {
        return MechanicalBeltShafts.shaftState(loot.getOptionalParameter(LootContextParams.BLOCK_ENTITY), entry.getDefaultState());
    }

    @Redirect(method = {"onRemove", "m_6810_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"),
            remap = false, require = 1, allow = 1)
    private BlockState cbjs$restoreOriginalShaft(BlockEntry<?> entry, @Local BlockEntity blockEntity) {
        // Create removes the entity before choosing the replacement; retain its local reference.
        return MechanicalBeltShafts.shaftState(blockEntity, entry.getDefaultState());
    }

    @Redirect(method = "getRequiredItems", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;asStack()Lnet/minecraft/world/item/ItemStack;"),
            remap = false, require = 1, allow = 1)
    private ItemStack cbjs$requireOriginalShaft(BlockEntry<?> entry, BlockState state, BlockEntity entity) {
        BlockState shaft = MechanicalBeltShafts.get(entity);
        return shaft == null ? entry.asStack() : MechanicalBeltShafts.shaftItem(shaft);
    }

    @Inject(method = "onWrenched", at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$removeCustomPulley(BlockState state, UseOnContext context,
            CallbackInfoReturnable<InteractionResult> callback) {
        if (state.getValue(BeltBlock.CASING) || state.getValue(BeltBlock.PART) != BeltPart.PULLEY) return;
        Level level = context.getLevel();
        BlockEntity entity = level.getBlockEntity(context.getClickedPos());
        BlockState shaft = MechanicalBeltShafts.get(entity);
        if (shaft == null) return;
        if (!level.isClientSide) {
            KineticBlockEntity.switchToBlockState(level, context.getClickedPos(), state.setValue(BeltBlock.PART, BeltPart.MIDDLE));
            MechanicalBeltShafts.set(level.getBlockEntity(context.getClickedPos()), null);
            Player player = context.getPlayer();
            if (player != null && !player.isCreative())
                player.getInventory().placeItemBackInInventory(MechanicalBeltShafts.shaftItem(shaft));
        }
        callback.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$installCustomPulley(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (player.isShiftKeyDown() || !player.mayBuild() || state.getValue(BeltBlock.PART) != BeltPart.MIDDLE) return;
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof ScriptedShaftBlock)) return;
        if (!level.isClientSide) {
            BlockState shaft = item.getBlock().defaultBlockState().setValue(BlockStateProperties.AXIS,
                    ((BeltBlock)state.getBlock()).getRotationAxis(state));
            KineticBlockEntity.switchToBlockState(level, pos, state.setValue(BeltBlock.PART, BeltPart.PULLEY));
            MechanicalBeltShafts.set(level.getBlockEntity(pos), shaft);
            if (!player.isCreative()) held.shrink(1);
        }
        callback.setReturnValue(InteractionResult.SUCCESS);
    }

    @Redirect(method = "canTransportObjects", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1, allow = 1)
    private static boolean cbjs$transportCustomBelts(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    @Inject(method = "initBelt", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$deferUnloadedCustomBelt(Level level, BlockPos pos, CallbackInfo callback) {
        if (level.isClientSide) return;
        if (!level.isLoaded(pos)) {
            callback.cancel();
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof ScriptedMechanicalBeltBlock
                && !MechanicalBeltLengths.initializationLoaded(level, pos)) callback.cancel();
    }

    @ModifyConstant(method = "initBelt", constant = @Constant(intValue = 1000), remap = false, require = 1, allow = 1)
    private static int cbjs$findFullCustomBeltStart(int original, Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ScriptedMechanicalBeltBlock
                ? MechanicalBeltLengths.initializationBudget(level, pos) : original;
    }

    @Inject(method = "getBeltChain", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cbjs$readFullCustomChain(LevelAccessor level, BlockPos pos,
            CallbackInfoReturnable<java.util.List<BlockPos>> callback) {
        if (!level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            callback.setReturnValue(java.util.List.of());
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof ScriptedMechanicalBeltBlock)
            callback.setReturnValue(MechanicalBeltLengths.scanChain(level, pos, true));
    }

    @Redirect(method = "initBelt", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 3, allow = 3)
    private static boolean cbjs$initializeSameFamily(BlockEntry<?> entry, BlockState state, Level level, BlockPos pos) {
        return MechanicalBeltCompat.recognizeBelt(entry, state)
                && MechanicalBeltCompat.sameFamily(state, level.getBlockState(pos));
    }

    @Redirect(method = "getBeltChain", at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 2, allow = 2)
    private static boolean cbjs$traverseSameFamily(BlockEntry<?> entry, BlockState state,
            LevelAccessor level, BlockPos controller) {
        return MechanicalBeltCompat.recognizeBelt(entry, state)
                && MechanicalBeltCompat.sameFamily(state, level.getBlockState(controller));
    }

    @Redirect(method = {"onRemove", "m_6810_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 1, allow = 1)
    private boolean cbjs$removeSameFamily(BlockEntry<?> entry, BlockState neighbor,
            BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        return MechanicalBeltCompat.recognizeBelt(entry, neighbor) && MechanicalBeltCompat.sameFamily(state, neighbor);
    }

    @Redirect(method = {"updateEntityAfterFallOn", "m_5548_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), remap = false, require = 2, allow = 2)
    private boolean cbjs$landOnCustomBelts(BlockEntry<?> entry, BlockState state) {
        return MechanicalBeltCompat.recognizeBelt(entry, state);
    }

    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/ItemEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false, require = 2, allow = 2)
    private boolean cbjs$useMatchingConnector(ItemEntry<?> entry, ItemStack item,
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return MechanicalBeltCompat.recognizeConnector(entry, item, state);
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void cbjs$rejectOtherConnectors(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callback) {
        if (player.isShiftKeyDown() || !player.mayBuild()) return;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BeltConnectorItem && !MechanicalBeltCompat.matchesConnector(state, held))
            callback.setReturnValue(InteractionResult.FAIL);
    }
}
