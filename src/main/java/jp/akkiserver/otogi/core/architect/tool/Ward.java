package jp.akkiserver.otogi.core.architect.tool;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.ArchitectItems;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;

/** One spell guard; normal weapon hits never consume it. */
public final class Ward extends BaseTool implements Tickable {
    private static final String BROKEN = "ward.broken";

    public Ward() {
        super("ward", "護符", Material.PAPER, 16,
                "手に持つ間、一度だけ術のダメージを防ぐ", "砕けた護符は時間が経つと戻る", "左クリック：護符を振る");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        CombatEffects.arc(self.getPlayer(), CombatSettings.number("tools.ward.reach", 2.6, 0.1, 10), 45);
        CombatEffects.sound(self.getPlayer().getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.4f);
        Targets.cone(self, CombatSettings.number("tools.ward.reach", 2.6, 0.1, 10), 45).stream().findFirst()
                .ifPresent(victim -> Combat.damage(self, victim, CombatSettings.number("tools.ward.damage", 3, 0.1, 40)));
        return true;
    }

    @Override
    public void onDamageTaken(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {
        if (!Combat.isSkillDamage() || event.isCancelled() || event.getDamage() <= 0 || self.getRemainingCoolTime(this) > 0
                || ArchitectItems.read(self.getPlayer().getInventory().getItemInMainHand()) != this) return;
        event.setCancelled(true);
        self.setCoolTime(this, CombatSettings.ticks("tools.ward.regenerate-ticks", 360));
        self.setState(BROKEN, true);
        CombatEffects.ring(self.getPlayer().getLocation().add(0, 1, 0), 0.8, Particle.ENCHANTED_HIT, 24);
        CombatEffects.sound(self.getPlayer().getLocation(), Sound.ITEM_SHIELD_BREAK, 0.85f, 1.5f);
        self.sendNotice(Component.text("護符が術を弾いた", NamedTextColor.AQUA));
    }

    @Override
    public void onTick(@NotNull ArenaPlayer self) {
        if (self.hasState(BROKEN) && self.getRemainingCoolTime(this) <= 0) {
            self.removeState(BROKEN);
            CombatEffects.sound(self.getPlayer().getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
            self.sendNotice(Component.text("護符が戻った", NamedTextColor.AQUA));
        }
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer self) { self.removeState(BROKEN); }
}
