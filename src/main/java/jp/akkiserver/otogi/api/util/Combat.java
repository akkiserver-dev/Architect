package jp.akkiserver.otogi.api.util;

import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

/**
 * Damage dealing for custom attacks.
 */
public final class Combat {
    private static boolean dealing;
    public enum DamageKind { TOOL, AREA_TOOL, SKILL, AREA_SKILL, SECONDARY }
    private static DamageKind damageKind = DamageKind.TOOL;

    public static DamageKind getDamageKind() { return damageKind; }
    public static boolean isSkillDamage() { return dealing && (damageKind == DamageKind.SKILL || damageKind == DamageKind.AREA_SKILL); }
    public static boolean isAreaDamage() { return dealing && (damageKind == DamageKind.AREA_TOOL || damageKind == DamageKind.AREA_SKILL); }
    public static boolean isSecondaryDamage() { return dealing && damageKind == DamageKind.SECONDARY; }

    /**
     * Returns whether a custom attack is currently applying damage.
     * Such damage must not be replaced by the attack of the tool again.
     */
    public static boolean isDealing() {
        return dealing;
    }

    /**
     * Damages the victim as the attacker. The damage still goes through the regular damage event,
     * so architect hooks and death handling apply.
     */
    public static boolean damage(@NotNull ArenaPlayer attacker, @NotNull ArenaPlayer victim, double amount) {
        return damage(attacker, victim, amount, DamageKind.TOOL);
    }

    /** Returns true only when damage landed and the victim remained in the same life; safe for follow-up effects. */
    public static boolean damage(@NotNull ArenaPlayer attacker, @NotNull ArenaPlayer victim, double amount, @NotNull DamageKind kind) {
        if (!Double.isFinite(amount) || amount <= 0 || attacker == victim || attacker.isEliminated() || victim.isEliminated() || attacker.getArena() != victim.getArena()
                || !attacker.getArena().isRunning() || !victim.getPlayer().isValid() || victim.getPlayer().isDead()
                || !attacker.getPlayer().isOnline() || !victim.getPlayer().isOnline()
                || !attacker.getArena().getPlayers().contains(attacker) || !attacker.getArena().getPlayers().contains(victim)
                || victim.isInvulnerable() || attacker.getPlayer().getWorld() != victim.getPlayer().getWorld()) return false;
        Player target = victim.getPlayer();
        target.setNoDamageTicks(0);
        boolean previous = dealing;
        DamageKind previousKind = damageKind;
        dealing = true;
        damageKind = kind;
        try {
            double before = target.getHealth() + target.getAbsorptionAmount();
            int deaths = victim.getDeaths();
            target.damage(amount, attacker.getPlayer());
            if (victim.getDeaths() == deaths && attacker.getArena().isRunning()
                    && target.getHealth() + target.getAbsorptionAmount() < before) {
                CombatEffects.impact(attacker.getPlayer(), target, kind == DamageKind.SKILL || kind == DamageKind.AREA_SKILL);
                if (CombatSettings.enabled("feedback.hit-marker")) {
                    double dealt = before - target.getHealth() - target.getAbsorptionAmount();
                    attacker.sendNotice(net.kyori.adventure.text.Component.text("✦ " + target.getName() + "  −"
                            + String.format(java.util.Locale.ROOT, "%.1f", dealt), net.kyori.adventure.text.format.NamedTextColor.AQUA), 8);
                }
                return true;
            }
            return false;
        } finally {
            dealing = previous;
            damageKind = previousKind;
        }
    }

    /**
     * Pushes the victim away from the attacker horizontally, with a little lift.
     */
    public static void knockback(@NotNull ArenaPlayer attacker, @NotNull ArenaPlayer victim, double strength) {
        if (!Double.isFinite(strength) || strength <= 0 || attacker.isEliminated() || victim.isEliminated() || victim.isInvulnerable() || !victim.getArena().isRunning()
                || attacker.getArena() != victim.getArena() || !victim.getArena().getPlayers().contains(victim)
                || !attacker.getArena().getPlayers().contains(attacker) || attacker.getPlayer().getWorld() != victim.getPlayer().getWorld()) return;
        Vector direction = victim.getPlayer().getLocation().toVector().subtract(attacker.getPlayer().getLocation().toVector()).setY(0);
        if (direction.lengthSquared() == 0) {
            direction = attacker.getPlayer().getLocation().getDirection().setY(0);
        }
        if (direction.lengthSquared() == 0) {
            return;
        }
        Vector push = direction.normalize().multiply(strength).setY(0.2D);
        victim.getPlayer().setVelocity(victim.getPlayer().getVelocity().add(push));
    }

    private Combat() {}
}
