package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;
import java.util.HashSet;
import java.util.UUID;

public final class Sever extends BaseArchitect implements Skill {
    public Sever() { super("sever", "断界", "前方に3秒間、長さ6ブロックの境界を張る", "触れた敵にダメージを与え、術を1秒間封じる"); }
    @Override public Material getIcon() { return Material.ECHO_SHARD; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 20); }
    @Override public boolean onUse(ArenaPlayer self) {
        var origin = self.getPlayer().getLocation();
        var forward = origin.getDirection().setY(0);
        if (forward.lengthSquared() < .01) forward.setZ(1);
        forward.normalize();
        var center = origin.clone().add(forward.clone().multiply(3));
        var side = new org.bukkit.util.Vector(-forward.getZ(), 0, forward.getX());
        double half = AbilitySupport.value(this, "length", 6) / 2;
        var start = center.clone().subtract(side.clone().multiply(half));
        var end = center.clone().add(side.clone().multiply(half));
        var hit = new HashSet<UUID>();
        AbilitySupport.pulse(center, Particle.REVERSE_PORTAL, Sound.BLOCK_BEACON_ACTIVATE);
        AbilitySupport.repeat(self, this, AbilitySupport.ticks(this, "duration", 3), 2, tick -> {
            if (tick % 4 == 0) {
                AbilitySupport.line(start.clone().add(0, .15, 0), end.clone().add(0, .15, 0), Particle.REVERSE_PORTAL);
                AbilitySupport.line(start.clone().add(0, 1.5, 0), end.clone().add(0, 1.5, 0), Particle.ENCHANT);
            }
            for (var target : Targets.sphere(self, center, half + 2)) {
                var delta = target.getPlayer().getLocation().toVector().subtract(center.toVector());
                if (Math.abs(delta.dot(forward)) > .8 || Math.abs(delta.dot(side)) > half + .3 || Math.abs(delta.getY()) > 2) continue;
                if (!hit.add(target.getPlayer().getUniqueId())) continue;
                if (!Combat.damage(self, target, AbilitySupport.value(this, "damage", 5), Combat.DamageKind.AREA_SKILL)) continue;
                self.setState("ability.hit.sever", org.bukkit.Bukkit.getCurrentTick());
                AbilitySupport.mark(target, "effects.silenced.until", AbilitySupport.ticks(this, "silence", 1));
                AbilitySupport.pulse(target.getPlayer().getLocation(), Particle.CRIT, Sound.BLOCK_GLASS_BREAK);
            }
            return true;
        });
        return true;
    }
}
