package net.quiwh.cbjs.drives;

import com.simibubi.create.AllTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Riding uses vanilla passengers: the client never supplies a position or velocity. */
public final class DriveRiding {
    public static boolean mount(ServerPlayer player, BlockPos member, double distance) {
        if (!Double.isFinite(distance) || player.isSpectator() || !player.isAlive() || player.isPassenger()
                || player.isShiftKeyDown() || !AllTags.AllItemTags.CHAIN_RIDEABLE.matches(player.getMainHandItem())
                || !player.serverLevel().hasChunkAt(member)) return false;
        DriveLoop loop = DriveManager.loopAt(player.level(), member);
        if (loop == null || !DriveManager.isActive(player.serverLevel(), loop)) return false;
        Vec3 point = loop.path().point(distance);
        double reach = player.blockInteractionRange() + .5;
        if (point.distanceToSqr(player.getEyePosition()) > reach * reach) return false;
        var vehicle = new DriveRideEntity(DriveEntities.RIDER.get(), player.level());
        vehicle.configure(loop, distance, player);
        if (!canTravel(player, player.position(), vehicle.position())) return false;
        if (!player.serverLevel().addFreshEntity(vehicle)) return false;
        if (player.startRiding(vehicle, true)) return true;
        vehicle.discard();
        return false;
    }

    /** Check the whole passenger movement, including the initial step onto the drive. */
    static boolean canTravel(Player player, Vec3 origin, Vec3 target) {
        var level = player.level();
        int steps = Math.max(1, (int) Math.ceil(target.subtract(origin).length() / .2));
        for (int step = 1; step <= steps; step++) {
            Vec3 point = origin.lerp(target, (double) step / steps);
            var bounds = player.getBoundingBox().move(point.subtract(player.position())).deflate(.01);
            if (!level.hasChunksAt(BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ),
                    BlockPos.containing(bounds.maxX, bounds.maxY, bounds.maxZ)) || !level.noCollision(player, bounds))
                return false;
        }
        return true;
    }
    private DriveRiding() {}
}
