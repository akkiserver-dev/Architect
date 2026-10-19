package jp.akkiserver.otogi.core.architect.tool;

import jp.akkiserver.otogi.core.combat.CombatGeometry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;

import java.util.List;
import java.util.UUID;

/** 具：細剣 */
public final class Rapier extends BaseTool {
    private static final String COMBO = "rapier.combo";
    private static double damage() { return CombatSettings.number("tools.rapier.damage", 3, 0.05, 40); }
    private static double backstabBonus() { return CombatSettings.number("tools.rapier.backstab-bonus", 2, 0.05, 40); }
    private static double length() { return CombatSettings.number("tools.rapier.length", 3.4, 0.05, 64); }
    private static double thickness() { return CombatSettings.number("tools.rapier.thickness", 0.3, 0.05, 40); }
    private static int comboGraceTicks() { return CombatSettings.ticks("tools.rapier.combo-grace-ticks", 40); }
    /** The attack that reaches this count pierces. */
    private static int pierceCount() { return CombatSettings.ticks("tools.rapier.pierce-count", 3); }

    public Rapier() {
        super("rapier", "細剣", Material.GOLDEN_SWORD, 6, "軽い突きを、素早く重ねる", "背後への一撃は威力が上がる", "同じ相手に突きを重ねると、次の一撃が貫通する");
    }

    private static final class Combo {
        UUID target;
        int count;
        int lastTick;
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        Player player = self.getPlayer();
        Combo combo = self.getState(COMBO, Combo.class);
        if (combo == null) {
            combo = new Combo();
            self.setState(COMBO, combo);
        }
        int now = Bukkit.getCurrentTick();
        boolean active = combo.target != null && now - combo.lastTick <= comboGraceTicks();
        List<ArenaPlayer> hits = Targets.line(self, length(), thickness());
        boolean sameTarget = !hits.isEmpty() && hits.getFirst().getPlayer().getUniqueId().equals(combo.target);
        boolean pierce = active && sameTarget && combo.count + 1 >= pierceCount();

        Location eye = player.getEyeLocation();
        CombatEffects.trace(eye, eye.getDirection(), length(), thickness(), pierce ? Particle.END_ROD : Particle.CRIT);
        CombatEffects.sound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8F, pierce ? 1.8F : 1.5F);

        if (hits.isEmpty()) {
            combo.target = null;
            combo.count = 0;
            return true;
        }

        if (pierce) {
            hits.forEach(victim -> strike(self, victim));
            combo.target = null;
            combo.count = 0;
            return true;
        }

        ArenaPlayer victim = hits.getFirst();
        strike(self, victim);
        UUID id = victim.getPlayer().getUniqueId();
        combo.count = active && id.equals(combo.target) ? combo.count + 1 : 1;
        combo.target = id;
        combo.lastTick = now;
        return true;
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) { player.removeState(COMBO); }

    private static void strike(ArenaPlayer self, ArenaPlayer victim) {
        boolean behind = isBehind(self.getPlayer(), victim.getPlayer());
        Combat.damage(self, victim, damage() + (behind ? backstabBonus() : 0.0D));
        Player target = victim.getPlayer();
        if (CombatSettings.enabled("feedback.particles")) target.getWorld().spawnParticle(behind ? Particle.ENCHANTED_HIT : Particle.DAMAGE_INDICATOR,
                target.getBoundingBox().getCenter().toLocation(target.getWorld()), behind ? 16 : 4, 0.2, 0.3, 0.2, 0.2);
    }

    private static boolean isBehind(Player attacker, Player victim) {
        Vector facing = victim.getLocation().getDirection().setY(0);
        Vector toAttacker = attacker.getLocation().toVector().subtract(victim.getLocation().toVector()).setY(0);
        return CombatGeometry.isBehind(facing.getX(), facing.getZ(), toAttacker.getX(), toAttacker.getZ(),
                CombatSettings.number("tools.rapier.backstab-half-angle", 45, 1, 90));
    }
}
