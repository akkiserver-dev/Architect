package jp.akkiserver.otogi.core.combat;

import jp.akkiserver.otogi.api.util.Targets;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** Small, bounded effects: clear silhouettes and close-range audio, without covering targets. */
public final class CombatEffects {
    public static void personalSound(Player player, Sound sound, float volume, float pitch) {
        if (CombatSettings.enabled("feedback.sound"))
            player.playSound(player.getLocation(), sound, SoundCategory.PLAYERS,
                    volume * (float) CombatSettings.number("feedback.volume", .7, 0, 2), pitch);
    }
    public static void sound(Location at, Sound sound, float volume, float pitch) {
        if (CombatSettings.enabled("feedback.sound"))
            at.getWorld().playSound(at, sound, SoundCategory.PLAYERS,
                    volume * (float) CombatSettings.number("feedback.volume", 0.7, 0, 2), pitch);
    }

    public static void impact(Player attacker, Player victim, boolean heavy) {
        if (CombatSettings.enabled("feedback.particles")) {
            Location point = victim.getBoundingBox().getCenter().toLocation(victim.getWorld());
            point.getWorld().spawnParticle(heavy ? Particle.ENCHANTED_HIT : Particle.CRIT,
                    point, heavy ? 9 : 5, 0.18, 0.25, 0.18, 0.08);
        }
        personalSound(attacker, Sound.BLOCK_NOTE_BLOCK_HAT, 0.35f, heavy ? 1.8f : 1.3f);
    }

    public static void arc(Player player, double radius, double halfAngle) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        Location origin = player.getEyeLocation();
        Vector forward = origin.getDirection().normalize();
        Vector right = new Vector(Math.cos(Math.toRadians(origin.getYaw())), 0, Math.sin(Math.toRadians(origin.getYaw())));
        for (int i = 0; i <= 14; i++) {
            double angle = Math.toRadians(-halfAngle + 2 * halfAngle * i / 14);
            Vector ray = forward.clone().multiply(Math.cos(angle)).add(right.clone().multiply(Math.sin(angle))).normalize();
            double visible = Targets.clearDistance(origin, ray, radius);
            Location point = origin.clone().add(ray.multiply(visible));
            origin.getWorld().spawnParticle(Particle.CRIT, point, 1, 0, 0, 0, 0);
        }
    }

    public static void ring(Location center, double radius, Particle particle, int points) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        for (int i = 0; i < Math.min(points, 32); i++) {
            double angle = i * Math.PI * 2 / points;
            center.getWorld().spawnParticle(particle, center.clone().add(Math.cos(angle) * radius, 0.08, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    /** A clipped central trace plus sparse width markers, using the same origin and radius as line targeting. */
    public static void trace(Location origin, Vector direction, double length, double radius, Particle particle) {
        if (!CombatSettings.enabled("feedback.particles") || !Double.isFinite(length) || length <= 0
                || !Double.isFinite(radius) || direction.lengthSquared() < 1e-8) return;
        Vector forward = direction.clone().normalize();
        double reach = Targets.clearDistance(origin, forward, Math.min(length, 64));
        Vector right = Math.abs(forward.getY()) > .99 ? new Vector(1, 0, 0) : forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
        Vector up = right.clone().crossProduct(forward).normalize();
        radius = Math.clamp(radius, 0, 2);
        double step = Math.max(.25, reach / 40);
        for (double distance = 0; distance <= reach; distance += step) {
            origin.getWorld().spawnParticle(particle, origin.clone().add(forward.clone().multiply(distance)), 1, 0, 0, 0, 0);
        }
        if (radius > .01) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * i / 4;
                Vector offset = right.clone().multiply(Math.cos(angle) * radius).add(up.clone().multiply(Math.sin(angle) * radius));
                Location edge = origin.clone().add(forward.clone().multiply(reach)).add(offset);
                Vector toEdge = edge.toVector().subtract(origin.toVector());
                if (Targets.clearDistance(origin, toEdge, toEdge.length()) + .02 >= toEdge.length())
                    origin.getWorld().spawnParticle(particle, edge, 1, 0, 0, 0, 0);
            }
        }
    }

    private CombatEffects() {}
}
