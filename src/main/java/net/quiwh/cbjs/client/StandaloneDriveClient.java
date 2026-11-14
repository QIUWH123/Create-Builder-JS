package net.quiwh.cbjs.client;

import com.simibubi.create.AllTags;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.quiwh.cbjs.content.MaterialGear;
import net.quiwh.cbjs.drives.*;

/** Client-only picking and previews. Actual placement, reach, inventory and movement are server-owned. */
@EventBusSubscriber(modid = "cbjs", value = Dist.CLIENT)
public final class StandaloneDriveClient {
    private static final Map<DriveCogwheelBlockEntity, Boolean> VISIBLE = new WeakHashMap<>();
    private static DriveNetworking.Selection selection;
    private static long selectedAt;
    private static ClientLevel previewLevel;
    private static BlockState[] previewStates;
    private static DrivePath previewPath;

    @EventBusSubscriber(modid = "cbjs", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(DriveEntities.RIDER.get(), NoopRenderer::new);
        }
    }
    public static void visible(DriveCogwheelBlockEntity entity) {
        if (entity.drive() != null) VISIBLE.put(entity, Boolean.TRUE);
    }
    public static void selection(DriveNetworking.Selection packet) {
        selection = packet.nodes().isEmpty() ? null : packet;
        selectedAt = System.nanoTime();
        resetPreview();
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) clear();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }

    private static void clear() {
        VISIBLE.clear(); selection = null; resetPreview(); StandaloneDriveRenderer.clear();
    }

    private static void resetPreview() { previewLevel = null; previewStates = null; previewPath = null; }

    @SubscribeEvent public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND || mc.player == null
                || mc.level == null || mc.screen != null
                || !AllTags.AllItemTags.CHAIN_RIDEABLE.matches(mc.player.getMainHandItem())) return;
        Vec3 origin = mc.player.getEyePosition();
        Vec3 direction = mc.player.getLookAngle();
        double reach = mc.player.blockInteractionRange();
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS)
            reach = Math.min(reach, mc.hitResult.getLocation().distanceTo(origin) + .05);
        DriveLoop best = null;
        double nearest = Double.POSITIVE_INFINITY;
        var seen = new java.util.HashSet<java.util.UUID>();
        for (DriveCogwheelBlockEntity entity : VISIBLE.keySet()) {
            if (entity.isRemoved() || entity.getLevel() != mc.level || !mc.level.isLoaded(entity.getBlockPos())) continue;
            DriveLoop loop = entity.drive();
            if (loop == null || !seen.add(loop.id())) continue;
            double at = loop.path().rayDistance(origin, direction, reach, .25);
            if (at < 0) continue;
            double distance = loop.path().point(at).distanceToSqr(origin);
            if (distance < nearest) { best = loop; nearest = distance; }
        }
        if (best != null) {
            // Pick a known loaded member so no server lookup can request an unloaded chunk.
            BlockPos member = null;
            for (DriveNode node : best.nodes()) if (mc.level.isLoaded(node.pos())) { member = node.pos(); break; }
            if (member == null) return;
            PacketDistributor.sendToServer(new DriveNetworking.Action(member, mc.player.isShiftKeyDown()));
            event.setCanceled(true);
            event.setSwingHand(true);
        }
    }

    @SubscribeEvent public static void preview(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || selection == null) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || System.nanoTime() - selectedAt > 120_000_000_000L) {
            selection = null; resetPreview(); return;
        }
        if (!selectedMaterial(mc.player.getMainHandItem()) && !selectedMaterial(mc.player.getOffhandItem())) return;
        updatePreview(mc.level);
        var poses = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        var buffers = mc.renderBuffers().bufferSource();
        var lines = buffers.getBuffer(RenderType.lines());
        for (BlockPos pos : selection.nodes()) {
            if (mc.level.isLoaded(pos))
                LevelRenderer.renderLineBox(poses, lines, new AABB(pos).inflate(.02), .3f, .85f, 1, 1);
        }
        if (previewPath != null) {
            List<Vec3> points = previewPath.samples();
            var pose = poses.last();
            for (int i = 1; i < points.size(); i++) {
                Vec3 a = points.get(i - 1), b = points.get(i), normal = b.subtract(a).normalize();
                lines.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(.3f, .85f, 1f, 1f)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
                lines.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(.3f, .85f, 1f, 1f)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            }
        }
        poses.popPose();
    }

    private static boolean selectedMaterial(ItemStack held) {
        return DriveKind.fromItem(held) == selection.kind()
                && BuiltInRegistries.ITEM.getKey(held.getItem()).equals(selection.material());
    }

    /** Compare at most sixteen immutable states; build sampled geometry only when they change. */
    private static void updatePreview(ClientLevel level) {
        boolean changed = previewLevel != level || previewStates == null
                || previewStates.length != selection.nodes().size();
        if (!changed) for (int i = 0; i < previewStates.length; i++) {
            BlockPos pos = selection.nodes().get(i);
            BlockState current = level.isLoaded(pos) ? level.getBlockState(pos) : null;
            if (current != previewStates[i]) { changed = true; break; }
        }
        if (!changed) return;
        previewLevel = level;
        previewStates = new BlockState[selection.nodes().size()];
        previewPath = null;
        var nodes = new ArrayList<DriveNode>();
        Direction.Axis axis = null;
        boolean valid = true;
        for (int i = 0; i < previewStates.length; i++) {
            BlockPos pos = selection.nodes().get(i);
            BlockState state = level.isLoaded(pos) ? level.getBlockState(pos) : null;
            previewStates[i] = state;
            if (state == null || !selection.kind().accepts(state)) { valid = false; continue; }
            Direction.Axis currentAxis = state.getValue(BlockStateProperties.AXIS);
            if (axis != null && axis != currentAxis) valid = false;
            axis = currentAxis;
            var gear = (MaterialGear) state.getBlock();
            nodes.add(new DriveNode(pos, gear.isLargeCog() ? 1 : gear.isFlanged() ? .5 : .625));
        }
        if (valid && nodes.size() >= 2) try {
            previewPath = DrivePath.create(axis, nodes);
        } catch (IllegalArgumentException ignored) { /* Invalid partial selections retain their node outlines. */ }
    }
    private StandaloneDriveClient() {}
}
