package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

/** 術：浄化 */
public final class Purge extends BaseArchitect implements Skill {
    public Purge() {
        super("purge", "浄化", "自身に付与されている状態異常をすべて解除する");
    }

    @Override
    public int getDefaultCoolTime() {
        return AbilitySupport.ticks(this, "cooldown", 30);
    }

    @Override
    public @NotNull Material getIcon() {
        return Material.SUGAR;
    }

    @Override
    public boolean onUse(@NotNull ArenaPlayer player) {
        for (PotionEffect effect : List.copyOf(player.getPlayer().getActivePotionEffects())) {
            PotionEffectType type = effect.getType();
            if (type.getEffectCategory() == PotionEffectType.Category.HARMFUL) {
                player.getPlayer().removePotionEffect(type);
            }
        }
        player.getPlayer().setFireTicks(0);
        player.getPlayer().setFreezeTicks(0);
        player.removeState("effects.silenced.until");
        player.removeState("effects.jammed.until");
        AbilitySupport.pulse(player.getPlayer().getLocation(), Particle.HAPPY_VILLAGER, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE);
        return true;
    }
}
