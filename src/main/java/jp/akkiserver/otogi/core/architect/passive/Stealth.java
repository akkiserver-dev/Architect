package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

/** 相：隠密 */
public final class Stealth extends BaseArchitect implements Passive, Tickable {
    private static final String ACTIVE = "stealth.active";
    private static final int DURATION_TICKS = 200;
    private static final int COOL_TIME_TICKS = 600;

    public Stealth() {
        super("stealth", "隠密", "攻撃を受けると一定時間透明になる", "自分が攻撃・術を使う、または攻撃を受けると解除", "透明中は自動回復しない / CT 30秒");
    }

    private boolean isActive(ArenaPlayer player) {
        return player.hasState(ACTIVE);
    }

    private void deactivate(ArenaPlayer player) {
        player.removeState(ACTIVE);
        player.getPlayer().removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    @Override
    public void onDamageTakenConfirmed(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {
        if (attacker == null) {
            return;
        }
        if (isActive(self)) {
            deactivate(self);
        } else if (self.getRemainingCoolTime(this) <= 0) {
            self.setState(ACTIVE, true);
            self.setCoolTime(this, COOL_TIME_TICKS);
            self.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, DURATION_TICKS, 0, false, false));
        }
    }

    @Override
    public void onDamageDealt(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @NotNull EntityDamageByEntityEvent event) {
        if (isActive(self)) {
            deactivate(self);
        }
    }

    @Override
    public void onSkillUse(@NotNull ArenaPlayer self, @NotNull Skill skill) {
        if (isActive(self)) {
            deactivate(self);
        }
    }

    @Override
    public boolean canRegenerate(@NotNull ArenaPlayer self) {
        return !isActive(self);
    }

    @Override
    public void onTick(@NotNull ArenaPlayer player) {
        if (isActive(player) && !player.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.removeState(ACTIVE);
        }
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) {
        if (isActive(player)) {
            deactivate(player);
        }
    }
}
