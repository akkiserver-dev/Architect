package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffectType;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Veil extends BaseArchitect implements Skill {
    private static final String CENTER = "veil.center", ACTIVE = "veil.until";
    public Veil() { super("veil", "帳", "その場に6秒間、半径5ブロックの帳を張る", "帳の中では発光を解除 / 外への与ダメージ -20%"); }
    @Override public Material getIcon() { return Material.BLACK_CANDLE; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 24); }
    @Override public boolean onUse(ArenaPlayer self) {
        var center = self.getPlayer().getLocation();
        self.setState(CENTER, center);
        int duration = AbilitySupport.ticks(this, "duration", 6);
        AbilitySupport.mark(self, ACTIVE, duration);
        AbilitySupport.pulse(center, Particle.SMOKE, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK);
        AbilitySupport.repeat(self, this, duration, 5, tick -> {
            if (self.getState(CENTER, Location.class) != center) return false;
            AbilitySupport.ring(center, AbilitySupport.value(this, "radius", 5), Particle.SMOKE);
            if (inside(self, center)) self.getPlayer().removePotionEffect(PotionEffectType.GLOWING);
            return true;
        });
        return true;
    }
    private boolean inside(ArenaPlayer player, Location center) {
        double radius = AbilitySupport.value(this, "radius", 5);
        return player.getPlayer().getWorld() == center.getWorld() && player.getPlayer().getLocation().distanceSquared(center) <= radius * radius;
    }
    @Override public void onDamageDealt(ArenaPlayer self, ArenaPlayer victim, EntityDamageByEntityEvent event) {
        var center = self.getState(CENTER, Location.class);
        if (center != null && AbilitySupport.timed(self, ACTIVE) && inside(self, center) && !inside(victim, center)) event.setDamage(event.getDamage() * .8);
    }
    @Override public void onUnequip(ArenaPlayer self) { self.removeState(CENTER); self.removeState(ACTIVE); }
}
