package net.quiwh.cbjs.compat.bnb.mixin.customchain;

import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.PlacingCogwheelChain;
import com.kipti.bnb.content.kinetics.cogwheel_chain.graph.CogwheelChainPathfinder;
import com.kipti.bnb.content.kinetics.cogwheel_chain.placement.ChainInteractionFailedException;
import com.kipti.bnb.content.kinetics.cogwheel_chain.types.CogwheelChainType;
import com.kipti.bnb.network.packets.from_client.PlaceCogwheelChainPacket;
import com.kipti.bnb.registry.core.BnbConfigs;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Validate material/type and geometry before native BnB consumes the selected item. */
@Mixin(value = PlaceCogwheelChainPacket.class, remap = false)
public abstract class BnbChainPacketMixin {
    @Shadow @Final private PlacingCogwheelChain worldSpacePartialChain;
    @Shadow @Final private CogwheelChainType chainType;
    @Shadow @Final private Holder<Item> chainItemType;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void cbjs$preflight(ServerPlayer player, CallbackInfo callback) {
        if (player == null || worldSpacePartialChain == null || worldSpacePartialChain.getSize() < 2
                || chainType == null || chainItemType == null
                || CogwheelChainType.COGWHEEL_TYPE_BY_ITEM.get(chainItemType.value()) != chainType) {
            callback.cancel();
            return;
        }
        if (worldSpacePartialChain.getSize() > BnbConfigs.server().COGWHEEL_MAX_NODE_COUNT.get()
                || worldSpacePartialChain.maxBounds() > BnbConfigs.server().COGWHEEL_MAX_BOUNDS.get()) {
            callback.cancel();
            return;
        }
        for (var node : worldSpacePartialChain.getNodes()) {
            if (!player.level().hasChunkAt(node.pos())
                    || net.quiwh.cbjs.drives.DriveManager.hasDrive(player.level(), node.pos())) {
                callback.cancel();
                return;
            }
        }
        if (worldSpacePartialChain.checkMissingNodesInLevel(player.level(), chainType)) {
            callback.cancel();
            return;
        }
        try {
            if (CogwheelChainPathfinder.buildChainPath(worldSpacePartialChain) == null) callback.cancel();
        } catch (ChainInteractionFailedException invalid) {
            callback.cancel();
        }
    }
}
