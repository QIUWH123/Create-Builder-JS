package net.quiwh.cbjs.drives;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.quiwh.cbjs.content.MaterialGear;

/** Original wheel and bracket entity, extended through Create's native remote-connection hooks. */
public final class DriveCogwheelBlockEntity extends BracketedKineticBlockEntity {
    private DriveLoop drive;
    private boolean driveActive;

    public DriveCogwheelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DriveLoop drive() {
        return level instanceof ServerLevel server ? DriveSavedData.get(server).loopAt(worldPosition) : drive;
    }

    public boolean driveActive() {
        return level instanceof ServerLevel server ? DriveManager.isActive(server, drive()) : driveActive;
    }

    void drive(DriveLoop loop, boolean active) {
        if (drive != loop) invalidateRenderBoundingBox();
        drive = loop;
        driveActive = loop != null && active;
    }

    @Override public void initialize() {
        DriveManager.initialize(this);
        super.initialize();
    }

    @Override public void tick() {
        DriveManager.tick(this);
        super.tick();
        DriveManager.checkLimits(this);
    }

    @Override public void onChunkUnloaded() {
        DriveManager.unload(this);
        super.onChunkUnloaded();
    }

    @Override public void remove() {
        DriveManager.removed(this);
        super.remove();
    }

    @Override
    public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
        List<BlockPos> result = super.addPropagationLocations(block, state, neighbours);
        DriveLoop loop = drive();
        // Keep loaded old neighbours available while disconnect detaches the previous graph.
        if (loop != null && level != null)
            for (DriveNode node : loop.nodes())
                if (!node.pos().equals(worldPosition) && level.isLoaded(node.pos()) && !result.contains(node.pos()))
                    result.add(node.pos());
        return result;
    }

    @Override
    public float propagateRotationTo(KineticBlockEntity target, BlockState from, BlockState to,
                                     BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs) {
        if (connected(target) && from.getBlock() instanceof MaterialGear source
                && to.getBlock() instanceof MaterialGear destination)
            return (source.isLargeCog() ? 2f : 1f) / (destination.isLargeCog() ? 2f : 1f);
        return super.propagateRotationTo(target, from, to, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isCustomConnection(KineticBlockEntity target, BlockState from, BlockState to) {
        return connected(target) || super.isCustomConnection(target, from, to);
    }

    private boolean connected(KineticBlockEntity target) {
        if (!(level instanceof ServerLevel server) || target == this || target.isRemoved()
                || target.getLevel() != level || !(target instanceof DriveCogwheelBlockEntity other)) return false;
        DriveLoop loop = drive();
        return loop != null && other.drive() == loop && DriveManager.isActive(server, loop);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        if (!clientPacket) return;
        DriveLoop loop = drive();
        if (loop != null) tag.put("CBJSDrive", loop.serialize(new CompoundTag()));
        tag.putBoolean("CBJSDriveActive", driveActive());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        // SavedData owns server loops. Schematic/block copies cannot manufacture new drives.
        drive = clientPacket && tag.contains("CBJSDrive", Tag.TAG_COMPOUND)
                ? DriveLoop.read(tag.getCompound("CBJSDrive")) : null;
        driveActive = drive != null && clientPacket && tag.getBoolean("CBJSDriveActive");
        invalidateRenderBoundingBox();
    }

    @Override protected AABB createRenderBoundingBox() {
        AABB bounds = super.createRenderBoundingBox();
        DriveLoop loop = drive();
        if (loop != null)
            for (int i = 0; i < loop.nodes().size(); i++)
                if (loop.nodes().get(i).pos().equals(worldPosition))
                    return bounds.minmax(loop.path().bounds(i));
        return bounds;
    }

    @Override public void transform(BlockEntity entity, StructureTransform transform) {
        super.transform(entity, transform);
        drive = null;
        driveActive = false;
    }
}
