package jp.akkiserver.otogi.core.architect.skill;

import jp.akkiserver.otogi.core.architect.park.Focus;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Anchor extends BaseArchitect implements Skill {
    public Anchor() { super("anchor", "錨撃", "錨を放ち、最初の敵を1.5秒間つなぎ止める", "射程16ブロック / 近づくと錨へ踏み込む"); }
    @Override public Material getIcon() { return Material.HEAVY_CORE; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 18); }
    @Override public boolean onUse(ArenaPlayer self) {
        double range = Focus.range(self, AbilitySupport.value(this, "range", 16));
        var eye = self.getPlayer().getEyeLocation();
        double distance = Targets.clearDistance(eye, eye.getDirection(), range);
        var hits = Targets.line(self, range, .45);
        var end = hits.isEmpty() ? eye.clone().add(eye.getDirection().multiply(distance)) : hits.getFirst().getPlayer().getLocation();
        AbilitySupport.line(eye, end, Particle.CRIT);
        AbilitySupport.pulse(end, Particle.ELECTRIC_SPARK, Sound.BLOCK_CHAIN_HIT);
        if (hits.isEmpty()) return true;
        var target = hits.getFirst();
        int targetLife = target.getLifeGeneration();
        if (!Combat.damage(self, target, AbilitySupport.value(this, "damage", 4), Combat.DamageKind.SKILL)) return true;
        self.setState("ability.hit.anchor", org.bukkit.Bukkit.getCurrentTick());
        var fixed = target.getPlayer().getLocation();
        AbilitySupport.repeat(self, this, AbilitySupport.ticks(this, "duration", 1.5), 2, tick -> {
            if (!AbilitySupport.active(target) || target.getLifeGeneration() != targetLife || target.getPlayer().getWorld() != fixed.getWorld()
                    || !self.getPlayer().hasLineOfSight(target.getPlayer())) return false;
            var current = target.getPlayer().getLocation();
            var delta = fixed.toVector().subtract(current.toVector());
            if (delta.lengthSquared() > 16) return false;
            if (delta.lengthSquared() > .08) target.getPlayer().setVelocity(delta.multiply(.7));
            if (tick % 6 == 0) AbilitySupport.ring(fixed, .8, Particle.ELECTRIC_SPARK);
            if (self.getPlayer().getLocation().distanceSquared(fixed) < 9) {
                var pull = fixed.toVector().subtract(self.getPlayer().getLocation().toVector());
                if (pull.lengthSquared() > .01) self.getPlayer().setVelocity(pull.normalize().multiply(.65).setY(.18));
                return false;
            }
            return true;
        });
        return true;
    }
}
