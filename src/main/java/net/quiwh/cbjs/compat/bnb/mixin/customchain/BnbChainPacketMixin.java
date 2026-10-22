package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.cogwheel_chain.graph.ChainInteractionFailedException;
import com.kipti.bnb.content.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.kipti.bnb.registry.BnbFeatureFlag;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainMaterialContext;
import net.quiwh.cbjs.compat.bnb.customchain.BnbChainPacketItems;
import net.quiwh.cbjs.content.ChainMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PlaceCogwheelChainPacket.class, remap = false)
public abstract class BnbChainPacketMixin {
    @Shadow PlacingCogwheelChain worldSpacePartialChain;
    @Shadow int priorityChainTakeHand;

    @WrapMethod(method = "handle(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V")
    private void cbjs$validateAndSelect(MinecraftServer server, ServerPlayer player, Operation<Void> original) {
        ItemStack held = BnbChainPacketItems.heldChain(player, priorityChainTakeHand);
        if (held.isEmpty() || worldSpacePartialChain == null || worldSpacePartialChain.getSize() < 2
                || !BnbFeatureFlag.COGWHEEL_CHAIN_DRIVES.get()) return;
        if (worldSpacePartialChain.maxBounds() > PlacingCogwheelChain.MAX_CHAIN_BOUNDS
                || !worldSpacePartialChain.checkMatchingNodesInLevel(player.level())) return;
        // Native BnB consumes before pathfinding. Preflight avoids consuming a custom chain for an invalid loop.
        try {
            if (CogwheelChainPathfinder.buildChainPath(worldSpacePartialChain) == null) return;
        } catch (ChainInteractionFailedException invalid) { return; }
        try (var ignored = BnbChainMaterialContext.placing(ChainMaterials.id(held))) {
            original.call(server, player);
        }
    }

    @Redirect(method = "handle(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;getChainsFromInventory(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;IZ)Z"))
    private boolean cbjs$takeChosenMaterial(Player player, ItemStack vanilla, int count, boolean simulate) {
        return ChainConveyorBlockEntity.getChainsFromInventory(player,
                ChainMaterials.stack(BnbChainMaterialContext.placing(), 1), count, simulate);
    }
}
