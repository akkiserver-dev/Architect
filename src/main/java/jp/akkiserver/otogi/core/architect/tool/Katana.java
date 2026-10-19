package jp.akkiserver.otogi.core.architect.tool;

import jp.akkiserver.otogi.core.combat.CombatSettings;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;

/** 具：刀 */
public final class Katana extends BaseTool {
    private static double damage() { return CombatSettings.number("tools.katana.damage", 7, 0.05, 40); }
    private static double range() { return CombatSettings.number("tools.katana.range", 3, 0.05, 64); }
    private static double halfAngle() { return CombatSettings.number("tools.katana.half-angle", 55, 0.05, 180); }
    private static double arcRadius() { return CombatSettings.number("tools.katana.arc-radius", 2.3, 0.05, 40); }

    public Katana() {
        // 攻撃速度はドキュメントが@TODOのため、剣と同じ約1.6/秒とする
        super("katana", "刀", Material.IRON_SWORD, 12, "間合いに入った相手を、ひと振りで斬る", "左クリック：斬撃");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        slash(self.getPlayer());
        // 一振りで最も近い敵1人を斬る
        Targets.cone(self, range(), halfAngle()).stream().findFirst().ifPresent(victim -> Combat.damage(self, victim, damage()));
        return true;
    }

    /** Draws a horizontal arc in front of the player. */
    private static void slash(Player player) {
        CombatEffects.arc(player, range(), halfAngle());
        CombatEffects.sound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
    }
}
