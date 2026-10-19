package jp.akkiserver.otogi.core.architect.tool;

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

/** 具：長柄 */
public final class Halberd extends BaseTool {
    private static double damage() { return CombatSettings.number("tools.halberd.damage", 6, 0.05, 40); }
    private static double range() { return CombatSettings.number("tools.halberd.range", 4, 0.05, 64); }
    private static double halfAngle() { return CombatSettings.number("tools.halberd.half-angle", 35, 0.05, 180); }
    private static double knockback() { return CombatSettings.number("tools.halberd.knockback", 0.35, 0.05, 40); }
    private static double thrustLength() { return CombatSettings.number("tools.halberd.thrust-length", 6, 0.05, 64); }
    private static double thrustThickness() { return CombatSettings.number("tools.halberd.thrust-thickness", 0.25, 0.05, 40); }
    private static int thrustCoolTime() { return CombatSettings.ticks("tools.halberd.thrust-cool-time", 80); }

    public Halberd() {
        super("halberd", "長柄", Material.IRON_AXE, 16, "通常攻撃の射程が長く、命中した敵を弱くノックバックさせる",
                "しゃがみ＋左クリック：前方を貫く突き");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        Player player = self.getPlayer();
        if (player.isSneaking() && self.getRemainingCoolTime(this) <= 0) {
            thrust(self);
            self.setCoolTime(this, thrustCoolTime());
            return true;
        }

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        CombatEffects.arc(player, range(), halfAngle());
        CombatEffects.sound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);

        Targets.cone(self, range(), halfAngle()).stream().findFirst().ifPresent(victim -> {
            if (Combat.damage(self, victim, damage())) Combat.knockback(self, victim, knockback());
        });
        return true;
    }

    /** A thin attack that pierces every enemy on the line. Each enemy is hit once. */
    private static void thrust(ArenaPlayer self) {
        Player player = self.getPlayer();
        Location eye = player.getEyeLocation();
        List<ArenaPlayer> hits = Targets.line(self, thrustLength(), thrustThickness());
        double length = Targets.clearDistance(eye, eye.getDirection(), thrustLength());
        CombatEffects.trace(eye, eye.getDirection(), length, thrustThickness(), Particle.END_ROD);
        CombatEffects.sound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.2F);
        hits.forEach(victim -> Combat.damage(self, victim, damage(), Combat.DamageKind.AREA_TOOL));
    }
}
