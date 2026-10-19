package jp.akkiserver.otogi.core.architect.skill;

import jp.akkiserver.otogi.core.architect.park.Focus;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Tether extends BaseArchitect implements Skill {
    public Tether() { super("tether", "縁結び", "狙った敵と5秒間つながる（射程12ブロック）", "距離が開くと引き寄せ、最後に鈍足を与える"); }
    @Override public Material getIcon() { return Material.LEAD; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 18); }
    @Override public boolean onUse(ArenaPlayer self) {
        var targets = Targets.line(self, Focus.range(self, AbilitySupport.value(this, "range", 12)), .55);
        if (targets.isEmpty()) { AbilitySupport.notice(self, "縁を結ぶ相手が見つからない"); return false; }
        var target = targets.getFirst();
        int targetLife = target.getLifeGeneration();
        int duration = AbilitySupport.ticks(this, "duration", 5);
        AbilitySupport.pulse(target.getPlayer().getLocation(), Particle.ENCHANT, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
        AbilitySupport.repeat(self, this, duration + 1, 2, tick -> {
            if (!AbilitySupport.active(target) || target.getLifeGeneration() != targetLife || target.getPlayer().getWorld() != self.getPlayer().getWorld()) return false;
            var from = self.getPlayer().getLocation().add(0, 1, 0);
            var to = target.getPlayer().getLocation().add(0, 1, 0);
            if (from.distanceSquared(to) > 24 * 24) return false;
            if (tick % 6 == 0) AbilitySupport.line(from, to, Particle.ENCHANT);
            var delta = from.toVector().subtract(to.toVector());
            if (delta.lengthSquared() > 7 * 7) target.getPlayer().setVelocity(delta.normalize().multiply(.5).setY(.12));
            if (tick + 2 >= duration) target.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, true));
            return true;
        });
        return true;
    }
}
