package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;

public final class Hack extends BaseArchitect implements Skill {
    public Hack() { super("hack", "惑乱", "周囲10ブロックの敵の術を5秒間封じる", "2秒間、敵の視界を3回揺さぶる"); }
    @Override public Material getIcon() { return Material.FERMENTED_SPIDER_EYE; }
    @Override public int getDefaultCoolTime() { return AbilitySupport.ticks(this, "cooldown", 20); }
    @Override public boolean onUse(ArenaPlayer self) {
        var targets = Targets.sphere(self, self.getPlayer().getLocation(), AbilitySupport.value(this, "radius", 10));
        var lives = new java.util.HashMap<ArenaPlayer, Integer>();
        targets.forEach(target -> lives.put(target, target.getLifeGeneration()));
        AbilitySupport.ring(self.getPlayer().getLocation(), AbilitySupport.value(this, "radius", 10), Particle.WITCH);
        AbilitySupport.pulse(self.getPlayer().getLocation(), Particle.WITCH, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE);
        for (var target : targets) {
            AbilitySupport.mark(target, "effects.silenced.until", AbilitySupport.ticks(this, "duration", 5));
            AbilitySupport.notice(target, "惑乱 — 術が封じられた");
        }
        AbilitySupport.repeat(self, this, 39, 13, tick -> {
            for (var target : targets) {
                if (!AbilitySupport.active(target) || target.getLifeGeneration() != lives.get(target)) continue;
                var player = target.getPlayer();
                float offset = tick % 2 == 0 ? 45 : -45;
                player.setRotation(player.getYaw() + offset, (float) Math.clamp(player.getPitch() + offset / 3, -75, 75));
            }
            return true;
        });
        return true;
    }
}
