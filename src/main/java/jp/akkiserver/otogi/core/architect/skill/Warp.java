package jp.akkiserver.otogi.core.architect.skill;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;
import jp.akkiserver.otogi.core.architect.park.Focus;

/** 術：神隠し */
public final class Warp extends BaseArchitect implements Skill {
    private static final double RANGE = 30.0D;

    public Warp() {
        super("warp", "神隠し", "視線の先へ一瞬で移動する（射程30ブロック）", "壁の手前で止まり、空中にも移動できる");
    }

    @Override
    public int getDefaultCoolTime() {
        return AbilitySupport.ticks(this, "cooldown", 15);
    }

    @Override
    public @NotNull Material getIcon() {
        return Material.AMETHYST_SHARD;
    }

    @Override
    public boolean onUse(@NotNull ArenaPlayer arenaPlayer) {
        Player player = arenaPlayer.getPlayer();
        Location eye = player.getEyeLocation();
        double range = Focus.range(arenaPlayer, AbilitySupport.value(this, "range", RANGE));
        RayTraceResult result = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), range);

        Location target;
        if (result == null || result.getHitBlock() == null) {
            target = eye.clone().add(eye.getDirection().multiply(range));
            target.subtract(0, player.getEyeHeight(), 0);
        } else {
            BlockFace face = result.getHitBlockFace() == null ? BlockFace.UP : result.getHitBlockFace();
            target = result.getHitBlock().getRelative(face).getLocation().add(0.5D, 0.0D, 0.5D);
            if (face == BlockFace.DOWN) {
                target.subtract(0, 1, 0);
            }
        }
        target.setYaw(eye.getYaw());
        target.setPitch(eye.getPitch());
        if (!safe(target)) {
            Location start = player.getLocation();
            var delta = target.toVector().subtract(start.toVector());
            double length = delta.length();
            if (length < .01) return false;
            delta.normalize();
            boolean found = false;
            for (double distance = length - .5; distance > .5; distance -= .5) {
                Location candidate = start.clone().add(delta.clone().multiply(distance));
                if (safe(candidate)) { target = candidate; found = true; break; }
            }
            if (!found) { AbilitySupport.notice(arenaPlayer, "移動できる足場がない"); return false; }
        }
        Location before = player.getLocation();
        if (!player.teleport(target)) return false;
        AbilitySupport.pulse(before, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
        AbilitySupport.pulse(target, Particle.PORTAL, Sound.ENTITY_ENDERMAN_TELEPORT);
        player.setFallDistance(0.0F);
        return true;
    }

    private static boolean safe(Location target) {
        if (target.getY() < target.getWorld().getMinHeight() || target.getY() + 1.8 >= target.getWorld().getMaxHeight()
                || !target.getWorld().getWorldBorder().isInside(target)) return false;
        for (double x : new double[]{-.3, .3}) for (double z : new double[]{-.3, .3}) {
            for (double y : new double[]{0, .9, 1.8}) {
                if (!target.clone().add(x, y, z).getBlock().isPassable()) return false;
            }
        }
        return true;
    }
}
