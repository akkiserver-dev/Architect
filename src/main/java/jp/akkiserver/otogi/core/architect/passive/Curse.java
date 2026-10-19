package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Curse extends BaseArchitect implements Passive {
    public Curse() { super("curse", "怨恨", "倒されたとき、相手に10秒間の怨霊を残す", "怨霊は発光と鈍足を与える / CT 40秒", "ジャミングを受けない"); }
    @Override public void onPlayerDeath(ArenaPlayer self, ArenaPlayer victim, ArenaPlayer killer) {
        Integer until = self.getMatchState("curse.until", Integer.class);
        if (self != victim || killer == null || killer == self || (until != null && until > org.bukkit.Bukkit.getCurrentTick())) return;
        self.setMatchState("curse.until", org.bukkit.Bukkit.getCurrentTick() + AbilitySupport.ticks(this, "cooldown", 40));
        int duration = AbilitySupport.ticks(this, "duration", 10);
        killer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration, 0, false, true));
        killer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 0, false, true));
        AbilitySupport.pulse(killer.getPlayer().getLocation(), Particle.SOUL, Sound.ENTITY_VEX_CHARGE);
        AbilitySupport.notice(killer, "怨霊に取り憑かれた");
    }
}
