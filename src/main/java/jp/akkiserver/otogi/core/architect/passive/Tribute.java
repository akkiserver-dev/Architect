package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

/** 相：献上 */
public final class Tribute extends BaseArchitect implements Passive, Tickable {
    private static final String REFERENCE = "tribute.reference";
    private static final double STEP = 0.20D;
    private static final int REDUCTION_TICKS = 20;

    public Tribute() {
        super("tribute", "献上", "体力が20%減少するたび、術のCTを1秒短縮", "1回のダメージで複数回発動しない / 自傷では発動しない", "体力が最大まで回復すると判定をリセット");
    }

    @Override
    public void onEquip(@NotNull ArenaPlayer player) {
        player.setState(REFERENCE, player.getMaxHealth());
    }

    @Override
    public void onDamageTakenConfirmed(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Double reference = self.getState(REFERENCE, Double.class);
        if (reference == null) {
            reference = self.getMaxHealth();
        }
        double after = self.getPlayer().getHealth() - event.getFinalDamage();
        if (reference - after >= self.getMaxHealth() * STEP) {
            self.reduceSkillCoolTimes(REDUCTION_TICKS);
            reference = after;
        }
        self.setState(REFERENCE, reference);
    }

    @Override
    public void onTick(@NotNull ArenaPlayer player) {
        if (player.getPlayer().getHealth() >= player.getMaxHealth()) {
            player.setState(REFERENCE, player.getMaxHealth());
        }
    }
}
