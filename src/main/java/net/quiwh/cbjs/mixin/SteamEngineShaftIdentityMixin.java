package net.quiwh.cbjs.mixin;

import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.quiwh.cbjs.content.SteamFamilies;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock.class, remap = false)
public abstract class SteamEngineShaftIdentityMixin {
    @Redirect(method = {"isShaftValid"}, at = @At(value = "INVOKE",
            target = "Lcom/tterrag/registrate/util/entry/BlockEntry;has(Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private static boolean cbjs$steamFamily(BlockEntry<?> entry, BlockState state) { return SteamFamilies.has(entry, state); }
    @org.spongepowered.asm.mixin.injection.Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, require = 1)
    private void cbjs$placeMaterialShaft(net.minecraft.world.item.ItemStack held, BlockState state, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.ItemInteractionResult> cir) {
        if (!(held.getItem() instanceof net.minecraft.world.item.BlockItem item)
                || !(item.getBlock() instanceof net.quiwh.cbjs.content.ScriptedShaftBlock)) return;
        var shaftPos = com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock.getShaftPos(state, pos);
        if (!level.getBlockState(shaftPos).canBeReplaced()) return;
        for (var direction : net.minecraft.core.Direction.orderedByNearest(player)) {
            var axis = direction.getAxis();
            if (axis == com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock.getFacing(state).getAxis()) continue;
            var offset = net.createmod.catnip.placement.PlacementOffset.success(shaftPos, placed -> {
                var oriented = placed.setValue(com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock.AXIS, axis);
                return level.isClientSide ? oriented : com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock.getEquivalent(oriented);
            });
            cir.setReturnValue(offset.placeInWorld(level, item, player, hand, hit));
            return;
        }
    }
}
