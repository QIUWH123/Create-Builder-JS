package net.quiwh.cbjs.drives;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.quiwh.cbjs.content.ChainMaterials;
import net.quiwh.cbjs.content.MaterialGear;

/** Server-owned drive transactions. Geometry and propagation never request unloaded chunks. */
public final class DriveManager {
    private enum State { VALID, PAUSED, INVALID }

    public static DriveLoop preview(ServerLevel level, List<BlockPos> positions, DriveKind kind,
                                    ResourceLocation material) {
        if (kind == null || !kind.validMaterial(material))
            throw new IllegalArgumentException("Unsupported drive material");
        if (positions == null || positions.size() < 2 || positions.size() > DriveLoop.MAX_NODES)
            throw new IllegalArgumentException("A drive needs between 2 and 16 cogwheels");
        DriveSavedData data = DriveSavedData.get(level);
        List<DriveNode> nodes = new ArrayList<>(positions.size());
        Direction.Axis axis = null;
        for (BlockPos pos : positions) {
            if (pos == null || !level.isLoaded(pos))
                throw new IllegalArgumentException("Every drive cogwheel must be loaded");
            BlockState state = level.getBlockState(pos);
            if (!kind.accepts(state) || !state.hasProperty(BlockStateProperties.AXIS)
                    || !(level.getBlockEntity(pos) instanceof DriveCogwheelBlockEntity))
                throw new IllegalArgumentException("Cogwheel does not accept this drive");
            if (data.loopAt(pos) != null)
                throw new IllegalArgumentException("Cogwheel already belongs to a drive");
            Direction.Axis wheelAxis = state.getValue(BlockStateProperties.AXIS);
            if (axis != null && wheelAxis != axis)
                throw new IllegalArgumentException("Drive cogwheels must have matching axes");
            axis = wheelAxis;
            nodes.add(new DriveNode(pos, radius((MaterialGear) state.getBlock())));
        }
        DriveLoop loop = new DriveLoop(UUID.randomUUID(), kind, material, axis, nodes, 0);
        ensureClear(level, loop, data);
        return loop;
    }

    public static DriveLoop connect(ServerLevel level, List<BlockPos> positions, DriveKind kind,
                                    ResourceLocation material, int paid) {
        DriveLoop preview = preview(level, positions, kind, material);
        DriveLoop loop = new DriveLoop(preview.id(), kind, material, preview.axis(), preview.nodes(), paid);
        DriveSavedData data = DriveSavedData.get(level);
        // Clear native neighbours before installing a second route between the endpoints.
        List<DriveCogwheelBlockEntity> endpoints = detachLoaded(level, loop);
        if (endpoints.size() != loop.nodes().size() || !loop.chunksLoaded(level))
            throw new IllegalArgumentException("A drive endpoint changed while connecting");
        for (int i = 0; i < endpoints.size(); i++) {
            BlockPos pos = loop.nodes().get(i).pos();
            if (!level.isLoaded(pos))
                throw new IllegalArgumentException("A drive endpoint unloaded while connecting");
            BlockState state = level.getBlockState(pos);
            if (level.getBlockEntity(pos) != endpoints.get(i) || endpoints.get(i).isRemoved()
                    || data.loopAt(pos) != null || !kind.accepts(state) || !state.hasProperty(BlockStateProperties.AXIS)
                    || state.getValue(BlockStateProperties.AXIS) != loop.axis()
                    || Math.abs(radius((MaterialGear) state.getBlock()) - loop.nodes().get(i).radius()) > 1e-7)
                throw new IllegalArgumentException("A drive endpoint changed while connecting");
        }
        data.add(loop);
        data.active(loop.id(), true);
        syncLoaded(level, loop, true);
        return loop;
    }

    public static DriveLoop loopAt(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) return DriveSavedData.get(server).loopAt(pos);
        return level != null && level.isLoaded(pos)
                && level.getBlockEntity(pos) instanceof DriveCogwheelBlockEntity entity ? entity.drive() : null;
    }

    public static boolean hasDrive(Level level, BlockPos pos) { return loopAt(level, pos) != null; }

    public static boolean isActive(ServerLevel level, DriveLoop loop) {
        if (loop == null) return false;
        DriveSavedData data = DriveSavedData.get(level);
        if (data.get(loop.id()) != loop || !data.isActive(loop.id()) || !loop.chunksLoaded(level)) return false;
        for (DriveNode node : loop.nodes()) if (!level.isLoaded(node.pos())) return false;
        return true;
    }

    /** Detach using the OLD topology; only its authoritative owner may remove/refund it. */
    public static void disconnect(ServerLevel level, UUID id, BlockPos refundAt, boolean refund) {
        DriveSavedData data = DriveSavedData.get(level);
        DriveLoop loop = data.get(id);
        if (loop == null || !data.beginDisconnect(id)) return;
        try {
            detachLoaded(level, loop);
            if (data.remove(id) == null) return;
            syncLoaded(level, loop, false);
            if (!refund || loop.paid() == 0 || !loop.kind().validMaterial(loop.material())) return;
            BlockPos drop = refundAt != null && level.isLoaded(refundAt) ? refundAt : null;
            if (drop == null)
                for (DriveNode node : loop.nodes()) if (level.isLoaded(node.pos())) { drop = node.pos(); break; }
            if (drop == null) return;
            ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(loop.material()));
            for (int left = loop.paid(); left > 0;) {
                int count = Math.min(left, material.getMaxStackSize());
                Block.popResource(level, drop, material.copyWithCount(count));
                left -= count;
            }
        } finally {
            data.endDisconnect(id);
        }
    }

    static void initialize(DriveCogwheelBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        DriveSavedData data = DriveSavedData.get(level);
        DriveLoop loop = data.loopAt(entity.getBlockPos());
        entity.drive(loop, loop != null && isActive(level, loop));
        if (loop != null) data.recheck(loop.id());
    }

    static void tick(DriveCogwheelBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        DriveSavedData data = DriveSavedData.get(level);
        DriveLoop loop = data.loopAt(entity.getBlockPos());
        if (loop == null) { entity.drive(null, false); return; }
        entity.drive(loop, isActive(level, loop));
        if (!data.needsCheck(loop.id(), level.getGameTime())) return;
        State state = state(level, loop, data);
        if (state == State.INVALID) {
            disconnect(level, loop.id(), entity.getBlockPos(), true);
        } else if (data.isActive(loop.id()) != (state == State.VALID)) {
            detachLoaded(level, loop);
            if (data.get(loop.id()) != loop) return;
            data.active(loop.id(), state == State.VALID);
            syncLoaded(level, loop, state == State.VALID);
        }
    }

    static void checkLimits(DriveCogwheelBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        DriveLoop loop = DriveSavedData.get(level).loopAt(entity.getBlockPos());
        if (loop != null && loop.kind() == DriveKind.CHAIN && ChainMaterials.isOverloaded(loop.material(), entity))
            disconnect(level, loop.id(), entity.getBlockPos(), true);
    }

    static void unload(DriveCogwheelBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        DriveSavedData data = DriveSavedData.get(level);
        DriveLoop loop = data.loopAt(entity.getBlockPos());
        if (loop == null) return;
        detachLoaded(level, loop);
        if (data.get(loop.id()) != loop) return;
        data.active(loop.id(), false);
        data.recheck(loop.id());
        syncLoaded(level, loop, false);
    }

    static void removed(DriveCogwheelBlockEntity entity) {
        if (!(entity.getLevel() instanceof ServerLevel level)) return;
        DriveLoop loop = DriveSavedData.get(level).loopAt(entity.getBlockPos());
        if (loop != null) disconnect(level, loop.id(), entity.getBlockPos(), true);
    }

    private static State state(ServerLevel level, DriveLoop loop, DriveSavedData data) {
        if (!loop.kind().validMaterial(loop.material())) return State.INVALID;
        boolean paused = !loop.chunksLoaded(level);
        for (DriveNode node : loop.nodes()) {
            if (!level.isLoaded(node.pos())) { paused = true; continue; }
            BlockState state = level.getBlockState(node.pos());
            if (!loop.kind().accepts(state) || !state.hasProperty(BlockStateProperties.AXIS)
                    || state.getValue(BlockStateProperties.AXIS) != loop.axis()
                    || Math.abs(radius((MaterialGear) state.getBlock()) - node.radius()) > 1e-7
                    || data.loopAt(node.pos()) != loop) return State.INVALID;
            if (!(level.getBlockEntity(node.pos()) instanceof DriveCogwheelBlockEntity)) paused = true;
        }
        return paused ? State.PAUSED : State.VALID;
    }

    private static double radius(MaterialGear gear) {
        return gear.isLargeCog() ? 1 : gear.isFlanged() ? .5 : .625;
    }

    private static List<DriveCogwheelBlockEntity> detachLoaded(ServerLevel level, DriveLoop loop) {
        List<DriveCogwheelBlockEntity> loaded = loaded(level, loop);
        for (DriveCogwheelBlockEntity entity : loaded) entity.detachKinetics();
        for (DriveCogwheelBlockEntity entity : loaded) {
            entity.removeSource();
            entity.updateSpeed = true;
        }
        return loaded;
    }

    private static void syncLoaded(ServerLevel level, DriveLoop loop, boolean active) {
        DriveSavedData data = DriveSavedData.get(level);
        for (DriveCogwheelBlockEntity entity : loaded(level, loop)) {
            entity.drive(data.get(loop.id()), active);
            entity.updateSpeed = true;
            entity.setChanged();
            entity.sendData();
        }
    }

    private static List<DriveCogwheelBlockEntity> loaded(ServerLevel level, DriveLoop loop) {
        List<DriveCogwheelBlockEntity> loaded = new ArrayList<>();
        for (DriveNode node : loop.nodes())
            if (level.isLoaded(node.pos()) && level.getBlockEntity(node.pos()) instanceof DriveCogwheelBlockEntity entity)
                loaded.add(entity);
        return loaded;
    }

    private static void ensureClear(ServerLevel level, DriveLoop loop, DriveSavedData data) {
        Set<BlockPos> wheels = new HashSet<>();
        for (DriveNode node : loop.nodes()) wheels.add(node.pos());
        Set<BlockPos> checked = new HashSet<>();
        for (Vec3 point : loop.path().samples()) {
            BlockPos pos = BlockPos.containing(point);
            if (!checked.add(pos) || wheels.contains(pos)) continue;
            if (!level.isLoaded(pos)) throw new IllegalArgumentException("The entire drive path must be loaded");
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty())
                throw new IllegalArgumentException("Drive path is obstructed");
        }
        AABB box = bounds(loop);
        for (DriveLoop existing : data.loops()) {
            if (!box.intersects(bounds(existing))) continue;
            Map<BlockPos, List<Vec3>> cells = new HashMap<>();
            for (Vec3 point : existing.path().samples())
                cells.computeIfAbsent(cell(point), ignored -> new ArrayList<>()).add(point);
            for (Vec3 point : loop.path().samples()) {
                BlockPos cell = cell(point);
                for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
                    List<Vec3> nearby = cells.get(cell.offset(x, y, z));
                    if (nearby == null) continue;
                    for (Vec3 other : nearby) if (point.distanceToSqr(other) < .04)
                        throw new IllegalArgumentException("Drive paths overlap");
                }
            }
        }
    }

    private static BlockPos cell(Vec3 point) {
        return BlockPos.containing(point.x * 4, point.y * 4, point.z * 4);
    }

    private static AABB bounds(DriveLoop loop) {
        AABB bounds = new AABB(loop.nodes().getFirst().pos()).inflate(1);
        for (DriveNode node : loop.nodes()) bounds = bounds.minmax(new AABB(node.pos()).inflate(1));
        return bounds;
    }

    private DriveManager() { }
}
