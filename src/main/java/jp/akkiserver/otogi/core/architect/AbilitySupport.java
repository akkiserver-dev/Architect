package jp.akkiserver.otogi.core.architect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;

import java.util.function.IntPredicate;

/** Small, bounded effects; every scheduled ability stops when its owner leaves the match. */
public final class AbilitySupport {
    public static double value(Architect owner, String key, double fallback) {
        double value = OtogiAPI.plugin.getConfig().getDouble("abilities." + owner.getId().getKey() + "." + key, fallback);
        return Double.isFinite(value) ? Math.clamp(value, 0, 1024) : fallback;
    }

    public static int ticks(Architect owner, String key, double seconds) {
        return (int) Math.clamp(Math.round(value(owner, key, seconds) * 20), 1, 72_000);
    }

    public static boolean active(ArenaPlayer player) {
        return !player.isEliminated() && player.getArena().isRunning() && player.getArena().getPlayers().contains(player)
                && player.getPlayer().isOnline() && !player.getPlayer().isDead();
    }

    public static boolean timed(ArenaPlayer player, String key) {
        Integer until = player.getState(key, Integer.class);
        return until != null && until > Bukkit.getCurrentTick();
    }

    public static void mark(ArenaPlayer player, String key, int ticks) {
        player.setState(key, Bukkit.getCurrentTick() + ticks);
    }

    public static void pulse(Location at, Particle particle, Sound sound) {
        if (CombatSettings.enabled("feedback.particles"))
            at.getWorld().spawnParticle(particle, at.clone().add(0, 1, 0), 22, .55, .65, .55, .03);
        CombatEffects.sound(at, sound, .7f, 1.2f);
    }

    public static void ring(Location at, double radius, Particle particle) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        for (int i = 0; i < 32; i++) {
            double angle = i * Math.PI / 16;
            at.getWorld().spawnParticle(particle, at.clone().add(Math.cos(angle) * radius, .15, Math.sin(angle) * radius), 1, 0, 0, 0, 0);
        }
    }

    public static void line(Location from, Location to, Particle particle) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        if (from.getWorld() != to.getWorld()) return;
        double distance = from.distance(to);
        int steps = Math.min(80, Math.max(1, (int) (distance * 3)));
        var delta = to.toVector().subtract(from.toVector()).multiply(1.0 / steps);
        for (int i = 0; i <= steps; i++) {
            from.getWorld().spawnParticle(particle, from.clone().add(delta.clone().multiply(i)), 1, 0, 0, 0, 0);
        }
    }

    public static void notice(ArenaPlayer player, String text) {
        player.sendNotice(Component.text(text, NamedTextColor.AQUA));
    }

    public static void repeat(ArenaPlayer player, Architect owner, int duration, int period, IntPredicate action) {
        String lifeKey = "ability.life." + owner.getId().getKey();
        Object life = player.getState(lifeKey, Object.class);
        if (life == null) { life = new Object(); player.setState(lifeKey, life); }
        Object expectedLife = life;
        new BukkitRunnable() {
            int elapsed;
            @Override public void run() {
                if (!active(player) || !player.has(owner) || player.getState(lifeKey, Object.class) != expectedLife
                        || elapsed >= duration || !action.test(elapsed)) {
                    cancel();
                    return;
                }
                elapsed += period;
            }
        }.runTaskTimer(OtogiAPI.plugin.getPlugin(), 1, period);
    }

    private AbilitySupport() {}
}
