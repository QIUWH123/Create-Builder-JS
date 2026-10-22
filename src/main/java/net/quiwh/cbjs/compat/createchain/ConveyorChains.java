package net.quiwh.cbjs.compat.createchain;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.quiwh.cbjs.content.ChainMaterials;

/** Retains native conveyor topology, package movement and chain cost for every chain material. */
public final class ConveyorChains {
    private ConveyorChains() { }

    public static ResourceLocation material(ChainConveyorBlockEntity conveyor, BlockPos relativeTarget) {
        if (conveyor instanceof ConveyorChainAccess access && access.cbjs$existingChains() != null)
            return access.cbjs$existingChains().material(relativeTarget);
        return ChainMaterials.VANILLA;
    }

    private static void material(ChainConveyorBlockEntity conveyor, BlockPos relativeTarget, ResourceLocation id) {
        if (ChainMaterials.VANILLA.equals(id)) { clearMaterial(conveyor, relativeTarget); return; }
        if (conveyor instanceof ConveyorChainAccess access) access.cbjs$chains().put(relativeTarget, id);
    }

    /** Native packets carry the selected item, but vanilla only remembers target positions. */
    public static void apply(ServerPlayer player, ChainConveyorBlockEntity source, BlockPos targetPos,
                             ItemStack chain, boolean connect) {
        var level = source.getLevel();
        if (level == null || level.isClientSide || !player.mayBuild() || !level.hasChunkAt(targetPos)) return;
        BlockPos sourcePos = source.getBlockPos();
        BlockPos relative = targetPos.subtract(sourcePos);
        if (sourcePos.equals(targetPos) || !sourcePos.closerThan(targetPos,
                AllConfigs.server().kinetics.maxChainConveyorLength.get() + 1.0)) return;
        if (!(level.getBlockEntity(targetPos) instanceof ChainConveyorBlockEntity target)) return;
        BlockPos reverse = relative.multiply(-1);

        if (!connect) {
            // A repeated or forged removal request must never mint chain items.
            if (!source.connections.contains(relative) || !target.connections.contains(reverse)) return;
            ResourceLocation material = material(source, relative);
            if (!player.isCreative()) refund(player, material, ChainConveyorBlockEntity.getChainCost(relative));
            source.chainDestroyed(relative, false, true);
            target.removeConnectionTo(sourcePos);
            source.removeConnectionTo(targetPos);
            level.playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS);
            return;
        }

        if (!ChainMaterials.isChain(chain) || !validGeometry(relative)) return;
        int maximum = AllConfigs.server().kinetics.maxChainConveyorConnections.get();
        if (source.connections.size() >= maximum || target.connections.size() >= maximum
                || source.connections.contains(relative) || target.connections.contains(reverse)) return;
        int cost = ChainConveyorBlockEntity.getChainCost(relative);
        if (!player.isCreative() && !ChainConveyorBlockEntity.getChainsFromInventory(player, chain, cost, true)) return;

        ResourceLocation selected = ChainMaterials.id(chain);
        // addConnectionTo sends a client update, so attach metadata before changing topology.
        material(source, relative, selected);
        material(target, reverse, selected);
        if (!target.addConnectionTo(sourcePos)) {
            clearMaterial(source, relative);
            clearMaterial(target, reverse);
            return;
        }
        if (!source.addConnectionTo(targetPos)) {
            target.removeConnectionTo(sourcePos);
            clearMaterial(source, relative);
            return;
        }
        if (!player.isCreative()) ChainConveyorBlockEntity.getChainsFromInventory(player, chain, cost, false);
    }

    public static boolean validGeometry(BlockPos difference) {
        Vec3 diff = Vec3.atLowerCornerOf(difference);
        if (diff.lengthSqr() < 2.5 * 2.5) return false;
        double horizontal = diff.multiply(1, 0, 1).length() - 1.5;
        return horizontal > 0 && Math.abs(diff.y) / horizontal <= 1;
    }

    public static void clearMaterial(ChainConveyorBlockEntity conveyor, BlockPos relative) {
        if (conveyor instanceof ConveyorChainAccess access && access.cbjs$existingChains() != null)
            access.cbjs$existingChains().remove(relative);
    }

    /** A chain rating belongs to its connection; exceeding it never destroys the conveyor. */
    public static void checkLimits(ChainConveyorBlockEntity conveyor) {
        var level = conveyor.getLevel();
        if (level == null || level.isClientSide || conveyor.isRemoved()
                || !(conveyor instanceof ConveyorChainAccess access)) return;
        var data = access.cbjs$existingChains();
        if (data == null) return;
        // This immutable cached list is empty for vanilla or unlimited materials, and remains
        // safe to traverse while native removal invalidates the underlying edge metadata.
        for (BlockPos relative : data.limitedConnections()) {
            if (!conveyor.connections.contains(relative)) continue;
            BlockPos targetPos = conveyor.getBlockPos().offset(relative);
            if (!level.hasChunkAt(targetPos)) continue;
            if (!(level.getBlockEntity(targetPos) instanceof ChainConveyorBlockEntity target)
                    || target.isRemoved() || !target.connections.contains(relative.multiply(-1))) continue;
            ResourceLocation material = data.material(relative);
            if (!ChainMaterials.isOverloaded(material, conveyor) && !ChainMaterials.isOverloaded(material, target)) continue;
            // Destruction needs the still-attached material to create the right drops and effect.
            // Native removal drops travelling packages and detaches/rebuilds both kinetic trees.
            conveyor.chainDestroyed(relative, true, true);
            target.removeConnectionTo(conveyor.getBlockPos());
            conveyor.removeConnectionTo(targetPos);
        }
    }

    public static void refund(Player player, ResourceLocation material, int amount) {
        while (amount > 0) {
            ItemStack stack = ChainMaterials.stack(material, 1);
            int count = Math.min(amount, stack.getMaxStackSize());
            stack.setCount(count);
            player.getInventory().placeItemBackInInventory(stack);
            amount -= count;
        }
    }

    public static void refundAll(Player player, ChainConveyorBlockEntity conveyor) {
        conveyor.cancelDrops = true;
        if (player.isCreative() || conveyor.getLevel() == null || conveyor.getLevel().isClientSide) return;
        for (BlockPos relative : conveyor.connections)
            refund(player, material(conveyor, relative), ChainConveyorBlockEntity.getChainCost(relative));
    }
}
