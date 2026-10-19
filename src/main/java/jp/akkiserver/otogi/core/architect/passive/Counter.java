package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Counter extends BaseArchitect implements Passive {
    private static final String ATTACKED = "counter.attacked.until";
    public Counter() { super("counter", "応報", "先手を取られると、相手を2秒間暗闇とジャミングにする", "CT 10秒 / 同じ相手には20秒に1回"); }
    @Override public void onDamageDealt(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        if (!Combat.isSecondaryDamage()) AbilitySupport.mark(self, ATTACKED, 200);
    }
    @Override public void onDamageTakenConfirmed(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) {
        if (attacker == null || event.isCancelled() || Combat.isSecondaryDamage()) return;
        String key = "counter.target." + attacker.getPlayer().getUniqueId();
        boolean ready = !AbilitySupport.timed(self, ATTACKED) && !AbilitySupport.timed(self, key) && self.getRemainingCoolTime(this) <= 0;
        AbilitySupport.mark(self, key, 400);
        if (!ready) return;
        self.setCoolTime(this, AbilitySupport.ticks(this, "cooldown", 10));
        int duration = AbilitySupport.ticks(this, "duration", 2);
        AbilitySupport.mark(attacker, "effects.jammed.until", duration);
        attacker.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, duration, 0, false, true));
        AbilitySupport.pulse(self.getPlayer().getLocation(), Particle.SOUL, Sound.BLOCK_SCULK_CATALYST_BLOOM);
        AbilitySupport.notice(self, "応報 — 先手を返した");
    }
}
