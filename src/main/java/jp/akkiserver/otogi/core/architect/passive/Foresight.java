package jp.akkiserver.otogi.core.architect.passive;

import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.util.Vector;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Foresight extends BaseArchitect implements Passive, Listener {
    public Foresight() { super("foresight", "先見", "自分へ向かう投射物の軌道が、一瞬だけ見える", "予兆から身をかわすと通常攻撃が再使用可能 / CT 12秒"); }
    /** Also accepts custom, simulated projectiles from tools. */
    public static void warn(ArenaPlayer attacker, ArenaPlayer target, Location origin, Vector direction) {
        Foresight passive = target.getArchitect(Foresight.class);
        if (passive == null || target.getRemainingCoolTime(passive) > 0 || direction.lengthSquared() < .01) return;
        var ray = direction.clone().normalize();
        var offset = target.getPlayer().getEyeLocation().toVector().subtract(origin.toVector());
        double along = offset.dot(ray);
        if (along < 0 || along > 32 || offset.clone().subtract(ray.clone().multiply(along)).lengthSquared() > 2.25) return;
        target.setCoolTime(passive, AbilitySupport.ticks(passive, "cooldown", 12));
        AbilitySupport.notice(target, "先見 — 射線が通っている");
        CombatEffects.personalSound(target.getPlayer(), Sound.BLOCK_NOTE_BLOCK_CHIME, .7f, 1.8f);
        if (CombatSettings.enabled("feedback.particles"))
            for (int i = 0; i < 14; i++) target.getPlayer().spawnParticle(Particle.END_ROD, origin.clone().add(ray.clone().multiply(along * i / 14)), 1, 0, 0, 0, 0);
        AbilitySupport.repeat(target, passive, 11, 10, tick -> {
            if (tick < 10 || target.getPlayer().getWorld() != origin.getWorld()) return true;
            var current = target.getPlayer().getEyeLocation().toVector().subtract(origin.toVector());
            double projection = Math.max(0, current.dot(ray));
            if (current.subtract(ray.clone().multiply(projection)).lengthSquared() > 2.25) {
                target.setAttackCoolTime(0);
                AbilitySupport.notice(target, "先見 — 反撃できる");
            }
            return false;
        });
    }
    @EventHandler(ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player player)) return;
        ArenaPlayer owner = OtogiAPI.getArenaPlayer(player);
        if (owner == null || !AbilitySupport.active(owner)) return;
        for (ArenaPlayer other : owner.getArena().getPlayers()) {
            if (other != owner && other.getPlayer().getWorld() == projectile.getWorld()) warn(owner, other, projectile.getLocation(), projectile.getVelocity());
        }
    }
}
