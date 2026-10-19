package jp.akkiserver.otogi.core.architect.park;

import jp.akkiserver.otogi.core.architect.skill.Sever;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

/** Range is queried by offensive abilities; missing a cast primes the next spell. */
public final class Focus extends BasePark {
    private static final String BONUS = "focus.bonus";
    public Focus() { super("focus", "研澄", new String[]{"8ブロック以内に敵がいなければ、術の射程 +10%", "術を外すと次の術のダメージ +10% / 導線と併用不可"}, "conduit"); }
    public static double range(ArenaPlayer self, double base) {
        Focus focus = self.getArchitect(Focus.class);
        return focus != null && Targets.sphere(self, self.getPlayer().getLocation(), AbilitySupport.value(focus, "isolation-radius", 8)).isEmpty()
                ? base * (1 + AbilitySupport.value(focus, "range-bonus", .1)) : base;
    }
    @Override public void onDamageDealt(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !Combat.isSkillDamage()) return;
        if (self.hasState(BONUS)) event.setDamage(event.getDamage() * (1 + AbilitySupport.value(this, "damage-bonus", .1)));
    }
    @Override public void onSkillUse(ArenaPlayer self, Skill skill) {
        String id = skill.getId().getKey();
        if (!id.equals("anchor") && !id.equals("sever")) return;
        int castTick = org.bukkit.Bukkit.getCurrentTick();
        if (id.equals("anchor")) {
            Integer hit = self.getState("ability.hit.anchor", Integer.class);
            if (hit != null && hit == castTick) self.removeState(BONUS);
            else self.setState(BONUS, true);
        } else {
            // Boundary damage is delayed; judge its miss only after the boundary closes.
            int duration = skill instanceof Sever
                    ? AbilitySupport.ticks(skill, "duration", 3) + 1 : 1;
            AbilitySupport.repeat(self, this, duration + 1, duration, tick -> {
                if (tick == 0) return true;
                Integer hit = self.getState("ability.hit.sever", Integer.class);
                if (hit != null && hit >= castTick) self.removeState(BONUS);
                else self.setState(BONUS, true);
                return false;
            });
        }
    }
    @Override public void onUnequip(ArenaPlayer self) { super.onUnequip(self); self.removeState(BONUS); }
}
