package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

/** 相：拾得 */
public final class Scavenger extends BaseArchitect implements Passive {
    private static final double RANGE = 12.0D;
    private static final int REDUCTION_TICKS = 20;

    public Scavenger() {
        super("scavenger", "拾得", "12b以内で敵が撃破されると術のCTを1秒短縮", "自身が撃破した場合は追加で1秒短縮");
    }

    @Override
    public void onPlayerDeath(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @Nullable ArenaPlayer killer) {
        if (self == victim) {
            return;
        }
        int reduction = 0;
        Location here = self.getPlayer().getLocation();
        Location there = victim.getPlayer().getLocation();
        if (here.getWorld() == there.getWorld() && here.distanceSquared(there) <= RANGE * RANGE) {
            reduction += REDUCTION_TICKS;
        }
        if (killer == self) {
            reduction += REDUCTION_TICKS;
        }
        if (reduction > 0) {
            self.reduceSkillCoolTimes(reduction);
        }
    }
}
