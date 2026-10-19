package jp.akkiserver.otogi.core.architect.passive;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.EntityEffect;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

/** 相：不死 */
public final class Resurrection extends BaseArchitect implements Passive {
    private static final String USED = "resurrection.used";
    private static final int INVULNERABLE_TICKS = 20;

    public Resurrection() {
        super("resurrection", "不死", "試合中1回だけ死を免れる", "体力が半分の状態で復活し、1秒間無敵");
    }

    @Override
    public boolean onFatalDamage(@NotNull ArenaPlayer self, @NotNull EntityDamageEvent event) {
        if (Boolean.TRUE.equals(self.getMatchState(USED, Boolean.class))) {
            return false;
        }
        self.setMatchState(USED, true);
        event.setCancelled(true);
        self.setHealth(self.getMaxHealth() / 2.0D);
        self.setInvulnerable(INVULNERABLE_TICKS);
        self.getPlayer().playEffect(EntityEffect.PROTECTED_FROM_DEATH);
        self.getPlayer().sendMessage(Component.text("不死が発動した", NamedTextColor.GOLD));
        return true;
    }
}
