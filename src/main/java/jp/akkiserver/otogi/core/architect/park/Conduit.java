package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Conduit extends BasePark {
    private static final String READY = "conduit.until";
    public Conduit() { super("conduit", "導線", new String[]{"術を当てると、次の通常攻撃のダメージ +25%", "強化は5秒間 / 共鳴と併用不可"}, "resonance"); }
    @Override public void onDamageDealt(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        if (event.isCancelled() || Combat.isSecondaryDamage()) return;
        if (!Combat.isSkillDamage() && AbilitySupport.timed(self, READY)) {
            event.setDamage(event.getDamage() * (1 + AbilitySupport.value(this, "bonus", .25)));
        }
    }
    @Override public void onDamageConfirmed(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        if (Combat.isSecondaryDamage()) return;
        if (Combat.isSkillDamage()) {
            AbilitySupport.mark(self, READY, AbilitySupport.ticks(this, "duration", 5));
            AbilitySupport.notice(self, "導線 — 次の一撃を強化");
        } else if (AbilitySupport.timed(self, READY)) {
            self.removeState(READY);
            AbilitySupport.pulse(victim.getPlayer().getLocation(), Particle.ELECTRIC_SPARK, Sound.BLOCK_AMETHYST_BLOCK_BREAK);
        }
    }
    @Override public void onUnequip(ArenaPlayer self) { super.onUnequip(self); self.removeState(READY); }
}
