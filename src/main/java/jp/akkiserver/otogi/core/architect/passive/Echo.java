package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Echo extends BaseArchitect implements Passive {
    public Echo() { super("echo", "反響", "受けた術ダメージの15%を、1秒後に相手へ返す", "CT 6秒 / 反響からは追加効果が発生しない"); }
    @Override public void onDamageTakenConfirmed(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) {
        if (attacker == null || event.isCancelled() || !Combat.isSkillDamage() || self.getRemainingCoolTime(this) > 0) return;
        self.setCoolTime(this, AbilitySupport.ticks(this, "cooldown", 6));
        double damage = event.getFinalDamage() * AbilitySupport.value(this, "ratio", .15);
        Object token = new Object();
        int attackerLife = attacker.getLifeGeneration();
        self.setState("echo.pending", token);
        Bukkit.getScheduler().runTaskLater(OtogiAPI.plugin.getPlugin(), () -> {
            if (!AbilitySupport.active(self) || !self.has(this) || !AbilitySupport.active(attacker)
                    || self.getState("echo.pending", Object.class) != token) return;
            if (attacker.getLifeGeneration() != attackerLife) return;
            self.removeState("echo.pending");
            Combat.damage(self, attacker, damage, Combat.DamageKind.SECONDARY);
            AbilitySupport.pulse(attacker.getPlayer().getLocation(), Particle.REVERSE_PORTAL, Sound.BLOCK_AMETHYST_BLOCK_RESONATE);
        }, 20);
    }
}
