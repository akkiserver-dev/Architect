package jp.akkiserver.otogi.core.architect.tool;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.ArchitectItems;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;

/** Handheld detection and a short, directional silence. No world blocks or potion effects are replaced. */
public final class Lantern extends BaseTool implements Tickable {
    public static final String SILENCED_UNTIL = "effects.silenced.until";

    public Lantern() {
        super("lantern", "灯籠", Material.LANTERN, 16,
                "手に持つと、近くに潜む相手の輪郭を照らす", "右クリック：前方の相手の術を封じる", "左クリック：灯籠を振る");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        CombatEffects.arc(self.getPlayer(), number("reach", 2.6), 45);
        CombatEffects.sound(self.getPlayer().getLocation(), Sound.BLOCK_CHAIN_HIT, 0.65f, 1.2f);
        Targets.cone(self, number("reach", 2.6), 45).stream().findFirst()
                .ifPresent(victim -> Combat.damage(self, victim, number("damage", 3)));
        return true;
    }

    @Override
    public boolean onUse(@NotNull ArenaPlayer self) {
        if (self.getRemainingCoolTime(this) > 0) return false;
        int duration = CombatSettings.ticks("tools.lantern.silence-ticks", 60);
        self.setCoolTime(this, CombatSettings.ticks("tools.lantern.cooldown-ticks", 320));
        var eye = self.getPlayer().getEyeLocation();
        trail(eye, eye.getDirection(), number("silence-range", 8), 0.4, Particle.END_ROD);
        CombatEffects.sound(eye, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1, 0.7f);
        for (ArenaPlayer victim : Targets.cone(self, number("silence-range", 8), 35)) {
            Integer previous = victim.getState(SILENCED_UNTIL, Integer.class);
            victim.setState(SILENCED_UNTIL, Math.max(previous == null ? 0 : previous, Bukkit.getCurrentTick() + duration));
            victim.sendNotice(Component.text("封殺 — 術が使えない", NamedTextColor.GOLD));
            CombatEffects.ring(victim.getPlayer().getLocation().add(0, 1, 0), 0.6, Particle.WAX_ON, 12);
            CombatEffects.sound(victim.getPlayer().getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.6f, 1.6f);
        }
        return true;
    }

    @Override
    public void onTick(@NotNull ArenaPlayer self) {
        if (ArchitectItems.read(self.getPlayer().getInventory().getItemInMainHand()) != this) return;
        double radius = number("reveal-range", 8);
        CombatEffects.ring(self.getPlayer().getLocation(), radius, Particle.WAX_ON, 24);
        for (ArenaPlayer other : Targets.sphere(self, self.getPlayer().getEyeLocation(), radius)) {
            if (!other.getPlayer().hasPotionEffect(PotionEffectType.INVISIBILITY)) continue;
            var at = other.getPlayer().getLocation();
            // A visible outline does not overwrite another architect's invisibility or glowing state.
            if (CombatSettings.enabled("feedback.particles")) {
                for (double y = 0.25; y <= 1.75; y += 0.5) {
                    at.getWorld().spawnParticle(Particle.END_ROD, at.clone().add(0, y, 0), 1, 0.18, 0.02, 0.18, 0);
                }
            }
        }
    }

    private static double number(String key, double fallback) {
        return CombatSettings.number("tools.lantern." + key, fallback, 0.1, 32);
    }
}
