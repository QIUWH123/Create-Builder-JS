package net.quiwh.cbjs.drives;

import com.simibubi.create.AllTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.quiwh.cbjs.compat.OptionalMods;

/** Selection is transient and server-owned; no inventory is consumed before closing a valid loop. */
public final class DriveInteraction {
    private record Selection(ResourceKey<Level> dimension, DriveKind kind, ResourceLocation material,
                             List<BlockPos> nodes, long expires) {}
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        var stack = event.getItemStack();
        DriveKind kind = DriveKind.fromItem(stack);
        boolean materialClick = !OptionalMods.bitsAndBobsLoaded() && kind != null
                && kind.accepts(level.getBlockState(pos));
        DriveLoop loop = DriveManager.loopAt(level, pos);
        boolean toolClick = loop != null && AllTags.AllItemTags.CHAIN_RIDEABLE.matches(stack);
        if (!materialClick && !toolClick) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (materialClick) select(serverPlayer, pos, event.getHand());
        else if (player.isShiftKeyDown()) {
            if (canEdit(serverPlayer, pos) && loop.nodes().stream().allMatch(node -> canEditLoaded(serverPlayer, node.pos())))
                DriveManager.disconnect(serverPlayer.serverLevel(), loop.id(), pos, true);
        } else {
            double distance = loop.path().nearestDistance(event.getHitVec().getLocation());
            DriveRiding.mount(serverPlayer, pos, distance);
        }
    }

    public static boolean select(ServerPlayer player, BlockPos pos, InteractionHand hand) {
        if (OptionalMods.bitsAndBobsLoaded() || !canEdit(player, pos)) return false;
        ServerLevel level = player.serverLevel();
        ItemStack held = player.getItemInHand(hand);
        DriveKind kind = DriveKind.fromItem(held);
        if (kind == null || !kind.accepts(level.getBlockState(pos))) return false;
        if (player.isShiftKeyDown()) { clear(player); message(player, "cancelled"); return true; }
        ResourceLocation material = BuiltInRegistries.ITEM.getKey(held.getItem());
        Selection selection = SELECTIONS.get(player.getUUID());
        if (selection == null || selection.dimension != level.dimension() || selection.kind != kind
                || !selection.material.equals(material) || selection.expires < level.getGameTime())
            selection = new Selection(level.dimension(), kind, material, List.of(), level.getGameTime() + 2400);
        var nodes = new ArrayList<>(selection.nodes);
        if (!nodes.isEmpty() && nodes.getFirst().equals(pos) && nodes.size() >= 2) {
            try {
                for (BlockPos node : nodes) if (!canEditLoaded(player, node))
                    throw new IllegalArgumentException("protected");
                DriveLoop preview = DriveManager.preview(level, nodes, kind, material);
                int cost = preview.cost();
                if (!player.isCreative() && count(player, held) < cost) {
                    player.displayClientMessage(Component.translatable("cbjs.drive.cost", cost), true);
                    return true;
                }
                // Reserve before native detach invokes script callbacks. Failed commits return only
                // these exact stacks, preserving unrelated inventory edits made by callbacks.
                List<ItemStack> reserved = player.isCreative() ? List.of() : reserve(player, held.copy(), cost);
                try {
                    DriveManager.connect(level, nodes, kind, material, player.isCreative() ? 0 : cost);
                } catch (RuntimeException failure) {
                    for (ItemStack stack : reserved) if (!player.getInventory().add(stack)) player.drop(stack, false);
                    player.containerMenu.broadcastChanges();
                    throw failure;
                }
                clear(player);
                message(player, "connected");
            } catch (IllegalArgumentException failure) {
                message(player, "invalid");
            }
            return true;
        }
        if (nodes.contains(pos) || nodes.size() >= 16 || DriveManager.loopAt(level, pos) != null) {
            message(player, "invalid"); return true;
        }
        if (!nodes.isEmpty() && !nodes.getFirst().closerThan(pos, 65)) {
            message(player, "invalid"); return true;
        }
        nodes.add(pos.immutable());
        SELECTIONS.put(player.getUUID(), new Selection(level.dimension(), kind, material,
                List.copyOf(nodes), level.getGameTime() + 2400));
        DriveNetworking.selection(player, kind, material, nodes);
        message(player, nodes.size() == 1 ? "first" : "next");
        return true;
    }

    private static int count(ServerPlayer player, ItemStack material) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, material)) total += stack.getCount();
        }
        return total;
    }

    private static List<ItemStack> reserve(ServerPlayer player, ItemStack material, int count) {
        var reserved = new ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize() && count > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!ItemStack.isSameItemSameComponents(stack, material)) continue;
            int amount = Math.min(count, stack.getCount());
            reserved.add(stack.split(amount));
            count -= amount;
        }
        if (count != 0) {
            for (ItemStack stack : reserved) if (!player.getInventory().add(stack)) player.drop(stack, false);
            throw new IllegalArgumentException("Drive inventory changed while reserving materials");
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return reserved;
    }

    public static boolean canEdit(ServerPlayer player, BlockPos pos) {
        return canEditLoaded(player, pos) && player.canInteractWithBlock(pos, 1);
    }

    private static boolean canEditLoaded(ServerPlayer player, BlockPos pos) {
        return !player.isSpectator() && player.mayBuild() && player.serverLevel().hasChunkAt(pos)
                && player.serverLevel().getWorldBorder().isWithinBounds(pos)
                && player.serverLevel().mayInteract(player, pos)
                && player.mayUseItemAt(pos, net.minecraft.core.Direction.UP, player.getMainHandItem());
    }

    /** A path action carries only a member; reach, ray hit and held tool are recomputed here. */
    public static void act(ServerPlayer player, BlockPos member, boolean remove) {
        if (!player.serverLevel().hasChunkAt(member) || !AllTags.AllItemTags.CHAIN_RIDEABLE.matches(player.getMainHandItem())) return;
        DriveLoop loop = DriveManager.loopAt(player.level(), member);
        if (loop == null) return;
        double reach = player.blockInteractionRange();
        var origin = player.getEyePosition();
        var direction = player.getLookAngle();
        double distance = loop.path().rayDistance(origin, direction, reach, .25);
        if (distance < 0) return;
        var point = loop.path().point(distance);
        var obstruction = player.level().clip(new net.minecraft.world.level.ClipContext(origin, point,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS
                && obstruction.getLocation().distanceToSqr(origin) + .1 < point.distanceToSqr(origin)) return;
        if (remove) {
            if (!player.isShiftKeyDown()) return;
            for (var node : loop.nodes()) if (!canEditLoaded(player, node.pos())) return;
            DriveManager.disconnect(player.serverLevel(), loop.id(), player.blockPosition(), true);
        } else if (!player.isShiftKeyDown()) DriveRiding.mount(player, member, distance);
    }

    public static void clear(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
        DriveNetworking.selection(player, DriveKind.CHAIN, DriveKind.CHAIN.defaultMaterial(), List.of());
    }
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) { SELECTIONS.remove(event.getEntity().getUUID()); }
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }
    public static void stopped(ServerStoppedEvent event) { SELECTIONS.clear(); }
    private static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("cbjs.drive." + key), true);
    }
    private DriveInteraction() {}
}
