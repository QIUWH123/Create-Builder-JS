package net.quiwh.cbjs.drives;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.quiwh.cbjs.content.MaterialGear;

/** A transient moving handhold. World connections, rather than the vehicle, own persistent state. */
public final class DriveRideEntity extends Entity {
    private UUID loopId;
    private BlockPos member = BlockPos.ZERO;
    private double distance;
    private Vec3 lastSafe;
    private Vec3 interpolationTarget;
    private int interpolationSteps;

    public DriveRideEntity(EntityType<? extends DriveRideEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvisible(true);
    }
    public void configure(DriveLoop loop, double at, Player player) {
        loopId = loop.id();
        member = loop.nodes().getFirst().pos();
        distance = at;
        lastSafe = player.position();
        Vec3 point = passengerPosition(loop, at, player.getBbHeight());
        setPos(point.x, point.y, point.z);
    }
    @Override public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (interpolationSteps > 0) {
                setPos(position().lerp(interpolationTarget, 1.0 / interpolationSteps));
                interpolationSteps--;
            }
            return;
        }
        if (!(level() instanceof ServerLevel level) || !(getFirstPassenger() instanceof Player player)
                || !player.isAlive() || player.isShiftKeyDown()
                || !AllTags.AllItemTags.CHAIN_RIDEABLE.matches(player.getMainHandItem())) { finish(); return; }
        DriveLoop loop = DriveManager.loopAt(level, member);
        if (loop == null || !loop.id().equals(loopId) || !DriveManager.isActive(level, loop)) { finish(); return; }
        if (!(level.getBlockEntity(member) instanceof KineticBlockEntity kinetic)) { finish(); return; }
        double radius = ((MaterialGear) kinetic.getBlockState().getBlock()).isLargeCog() ? 1 : .5;
        double motion = kinetic.getSpeed() * (Math.PI * 2 * radius / 1200) * loop.path().winding();
        if (!Double.isFinite(motion) || Math.abs(motion) > 4) { finish(); return; }
        double nextDistance = distance + motion;
        Vec3 target = passengerPosition(loop, nextDistance, player.getBbHeight());
        if (!DriveRiding.canTravel(player, position(), target)) { finish(); return; }
        distance = nextDistance % loop.path().length();
        setPos(target);
        player.fallDistance = 0;
        player.resetFallDistance();
        lastSafe = target;
    }
    /** Hang beside horizontal axles so the passenger can travel around the wheel itself. */
    public static Vec3 passengerPosition(DriveLoop loop, double distance, double height) {
        Vec3 offset = switch (loop.axis()) {
            case X -> new Vec3(.85, 0, 0);
            case Y -> Vec3.ZERO;
            case Z -> new Vec3(0, 0, .85);
        };
        // A vertical axle extends to the bottom of its block; hang below that shaft.
        double gap = loop.axis() == net.minecraft.core.Direction.Axis.Y ? .6 : .35;
        return loop.path().point(distance).add(offset).add(0, -height - gap, 0);
    }
    @Override public boolean shouldRiderSit() { return false; }
    private void finish() { ejectPassengers(); discard(); }
    @Override protected void positionRider(Entity passenger, MoveFunction movement) {
        if (hasPassenger(passenger)) movement.accept(passenger, getX(), getY(), getZ());
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 safe = lastSafe == null ? position() : lastSafe;
        return level().noCollision(passenger, passenger.getBoundingBox().move(safe.subtract(passenger.position())))
                ? safe : super.getDismountLocationForPassenger(passenger);
    }
    @Override public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        interpolationTarget = new Vec3(x, y, z);
        interpolationSteps = Math.max(1, steps);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
