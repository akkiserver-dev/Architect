package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

/** 術：過速 */
public final class Overclock extends BaseArchitect implements Skill, Tickable {
    private static final String REMAINING = "overclock.remaining";
    private static final int DURATION_SECONDS = 4;

    public Overclock() {
        super("overclock", "過速", "4秒間、自身の術のCT進行速度が2倍になる", "効果中に術を使用すると効果時間が1秒短縮");
    }

    @Override
    public int getDefaultCoolTime() {
        return AbilitySupport.ticks(this, "cooldown", 22);
    }

    @Override
    public @NotNull Material getIcon() {
        return Material.CLOCK;
    }

    @Override
    public boolean onUse(@NotNull ArenaPlayer player) {
        player.setState(REMAINING, DURATION_SECONDS);
        player.setCoolTimeRate(2.0D);
        AbilitySupport.pulse(player.getPlayer().getLocation(), Particle.ELECTRIC_SPARK, Sound.BLOCK_BEACON_POWER_SELECT);
        return true;
    }

    @Override
    public void onSkillUse(@NotNull ArenaPlayer self, @NotNull Skill skill) {
        Integer remaining = self.getState(REMAINING, Integer.class);
        if (remaining != null && skill != this) {
            end(self, remaining - 1);
        }
    }

    @Override
    public void onTick(@NotNull ArenaPlayer player) {
        Integer remaining = player.getState(REMAINING, Integer.class);
        if (remaining != null) {
            end(player, remaining - 1);
        }
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) {
        end(player, 0);
    }

    private void end(ArenaPlayer player, int remaining) {
        if (remaining > 0) {
            player.setState(REMAINING, remaining);
        } else {
            player.removeState(REMAINING);
            player.setCoolTimeRate(1.0D);
        }
    }
}
