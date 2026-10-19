package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Mirror extends BaseArchitect implements Skill {
    private static final String ACTIVE = "mirror.until";
    public Mirror() { super("mirror", "鏡界", "前方に2秒間、攻撃を一度だけ返す鏡を張る", "背後からの攻撃は防げない"); }
    @Override public Material getIcon() { return Material.GLASS_PANE; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 20); }
    @Override public boolean onUse(ArenaPlayer self) {
        int duration = AbilitySupport.ticks(this, "duration", 2);
        AbilitySupport.mark(self, ACTIVE, duration);
        AbilitySupport.pulse(self.getPlayer().getLocation(), Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_RESONATE);
        AbilitySupport.repeat(self, this, duration, 4, tick -> {
            if (!AbilitySupport.timed(self, ACTIVE)) return false;
            var eye = self.getPlayer().getEyeLocation();
            AbilitySupport.ring(eye.add(eye.getDirection().multiply(2)), 1, Particle.END_ROD);
            return true;
        });
        return true;
    }
    @Override public void onDamageTaken(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) {
        if (attacker == null || event.isCancelled() || !AbilitySupport.timed(self, ACTIVE) || Combat.isSecondaryDamage()) return;
        if (Combat.isAreaDamage()) {
            self.removeState(ACTIVE);
            AbilitySupport.pulse(self.getPlayer().getLocation(), Particle.END_ROD, Sound.BLOCK_GLASS_BREAK);
            return;
        }
        var toward = attacker.getPlayer().getLocation().toVector().subtract(self.getPlayer().getLocation().toVector());
        if (toward.lengthSquared() > .01 && toward.normalize().dot(self.getPlayer().getEyeLocation().getDirection()) < .2) return;
        self.removeState(ACTIVE);
        double reflected = event.getFinalDamage();
        event.setCancelled(true);
        AbilitySupport.pulse(self.getPlayer().getLocation(), Particle.END_ROD, Sound.BLOCK_GLASS_BREAK);
        Combat.damage(self, attacker, reflected, Combat.DamageKind.SECONDARY);
    }
    @Override public void onUnequip(ArenaPlayer self) { self.removeState(ACTIVE); }
}
