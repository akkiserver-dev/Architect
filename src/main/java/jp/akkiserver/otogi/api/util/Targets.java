package jp.akkiserver.otogi.api.util;

import jp.akkiserver.otogi.core.combat.CombatGeometry;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Hit detection for custom attacks. Every method only returns opponents of the owner in the same arena,
 * nearest first.
 */
public final class Targets {
    /**
     * Opponents inside a cone from the owner's eyes.
     *
     * @param range        reach from the eyes to the nearest point of the opponent's hitbox
     * @param halfAngleDeg half of the cone angle in degrees
     */
    @NotNull
    public static List<ArenaPlayer> cone(@NotNull ArenaPlayer owner, double range, double halfAngleDeg) {
        Player player = owner.getPlayer();
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        List<Hit> hits = new ArrayList<>();
        for (ArenaPlayer other : opponents(owner)) {
            BoundingBox box = other.getPlayer().getBoundingBox();
            Vector contact = CombatGeometry.coneContact(box, eye.toVector(), direction, range, halfAngleDeg);
            if (contact != null && !blocked(eye, contact)) hits.add(new Hit(other, contact.distance(eye.toVector())));
        }
        return sorted(hits);
    }

    /**
     * Opponents on a thick line from the owner's eyes. The line stops at the first block.
     *
     * @param length maximum length
     * @param radius thickness added around the line
     */
    @NotNull
    public static List<ArenaPlayer> line(@NotNull ArenaPlayer owner, double length, double radius) {
        Location eye = owner.getPlayer().getEyeLocation();
        return line(owner, eye, eye.getDirection(), length, radius);
    }

    @NotNull
    public static List<ArenaPlayer> line(@NotNull ArenaPlayer owner, @NotNull Location origin, @NotNull Vector direction,
                                         double length, double radius) {
        if (!Double.isFinite(length) || length <= 0 || !Double.isFinite(radius) || radius < 0 || direction.lengthSquared() < 0.000001D) return List.of();
        direction = direction.clone().normalize();
        double reach = clearDistance(origin, direction, length);
        List<Hit> hits = new ArrayList<>();
        for (ArenaPlayer other : opponents(owner)) {
            if (other.getPlayer().getWorld() != origin.getWorld()) {
                continue;
            }
            RayTraceResult result = other.getPlayer().getBoundingBox().expand(radius).rayTrace(origin.toVector(), direction, reach);
            if (result != null) {
                hits.add(new Hit(other, result.getHitPosition().distance(origin.toVector())));
            }
        }
        return sorted(hits);
    }

    /**
     * Opponents whose hitbox touches a sphere.
     */
    @NotNull
    public static List<ArenaPlayer> sphere(@NotNull ArenaPlayer owner, @NotNull Location center, double radius) {
        List<Hit> hits = new ArrayList<>();
        for (ArenaPlayer other : opponents(owner)) {
            if (other.getPlayer().getWorld() != center.getWorld()) {
                continue;
            }
            double distance = nearestPoint(other.getPlayer().getBoundingBox(), center).distance(center.toVector());
            if (distance <= radius && !blocked(center, other.getPlayer().getBoundingBox().getCenter())) {
                hits.add(new Hit(other, distance));
            }
        }
        return sorted(hits);
    }

    /**
     * Gets how far a ray can travel before it hits a block.
     */
    public static double clearDistance(@NotNull Location origin, @NotNull Vector direction, double length) {
        if (!Double.isFinite(length) || length <= 0 || direction.lengthSquared() < 0.000001D) return 0;
        World world = origin.getWorld();
        RayTraceResult block = world.rayTraceBlocks(origin, direction, length, FluidCollisionMode.NEVER, true);
        return block == null ? length : block.getHitPosition().distance(origin.toVector());
    }

    private static Vector nearestPoint(BoundingBox box, Location point) {
        return new Vector(
                Math.clamp(point.getX(), box.getMinX(), box.getMaxX()),
                Math.clamp(point.getY(), box.getMinY(), box.getMaxY()),
                Math.clamp(point.getZ(), box.getMinZ(), box.getMaxZ()));
    }

    private static boolean blocked(Location eye, Vector target) {
        Vector delta = target.clone().subtract(eye.toVector());
        double distance = delta.length();
        if (distance < 0.01D) {
            return false;
        }
        return eye.getWorld().rayTraceBlocks(eye, delta, distance, FluidCollisionMode.NEVER, true) != null;
    }

    private static List<ArenaPlayer> opponents(ArenaPlayer owner) {
        if (owner.isEliminated()) return List.of();
        List<ArenaPlayer> result = new ArrayList<>();
        for (ArenaPlayer other : owner.getArena().getPlayers()) {
            if (other != owner && !other.isEliminated() && other.getPlayer().isValid() && !other.getPlayer().isDead() && !other.isInvulnerable()
                    && other.getPlayer().getWorld() == owner.getPlayer().getWorld()) {
                result.add(other);
            }
        }
        return result;
    }

    private static List<ArenaPlayer> sorted(List<Hit> hits) {
        hits.sort(Comparator.comparingDouble(Hit::distance));
        return hits.stream().map(Hit::player).toList();
    }

    private record Hit(ArenaPlayer player, double distance) {}

    private Targets() {}
}
