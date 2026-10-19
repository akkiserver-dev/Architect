package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import java.util.UUID;

/** 相：首狩り */
public final class Bounty extends BaseArchitect implements Passive {
    private static final double THRESHOLD = 0.30D;
    private static final int REDUCTION_TICKS = 60;

    public Bounty() {
        super("bounty", "首狩り", "体力が30%以下の敵へ与えるダメージ +10%", "その敵を撃破すると術のCTを3秒短縮");
    }

    @Override
    public void onDamageDealt(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @NotNull EntityDamageByEntityEvent event) {
        if (victim.getPlayer().getHealth() <= victim.getMaxHealth() * THRESHOLD) {
            event.setDamage(event.getDamage() * 1.1D);
        }
    }

    @Override
    public void onKill(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim) {
        UUID marked = self.getState("bounty.target", UUID.class);
        if (victim.getPlayer().getUniqueId().equals(marked)) self.reduceSkillCoolTimes(REDUCTION_TICKS);
        self.removeState("bounty.target");
    }

    @Override public void onDamageConfirmed(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        self.removeState("bounty.target");
        if (victim.getPlayer().getHealth() <= victim.getMaxHealth() * THRESHOLD)
            self.setState("bounty.target", victim.getPlayer().getUniqueId());
    }
}
