package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Modifiers;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Resonance extends BasePark {
    private static final String READY = "resonance.ready";
    public Resonance() { super("resonance", "共鳴", new String[]{"術を当てると、次に使う術のCTを2秒短縮", "8秒に1回 / 最大体力 -2 / 金城と併用不可"}, "bulwark"); }
    @Override public void onEquip(ArenaPlayer self) { Modifiers.add(self.getPlayer(), Attribute.MAX_HEALTH, this, -2, AttributeModifier.Operation.ADD_NUMBER); }
    @Override public void onDamageConfirmed(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !Combat.isSkillDamage() || self.getRemainingCoolTime(this) > 0) return;
        self.setCoolTime(this, AbilitySupport.ticks(this, "interval", 8));
        // onSkillUse runs after synchronous damage. Arm on the next tick so this cast cannot consume it.
        AbilitySupport.repeat(self, this, 1, 1, tick -> {
            self.setState(READY, true);
            AbilitySupport.notice(self, "共鳴 — 次の術の戻りが早まる");
            return false;
        });
    }
    @Override public void onSkillUse(ArenaPlayer self, Skill skill) {
        if (self.hasState(READY)) {
            self.removeState(READY);
            self.reduceCoolTime(skill, AbilitySupport.ticks(this, "reduction", 2));
        }
    }
    @Override public void onUnequip(ArenaPlayer self) { super.onUnequip(self); self.removeState(READY); }
}
