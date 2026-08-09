package uk.akkiserver.otogi.api.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;
import uk.akkiserver.otogi.api.OtogiAPI;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

public record Position(double x, double y, double z, float yaw, float pitch, @Nullable UUID worldId) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static Position of(Location location) {
        UUID worldId = null;
        try {
            final World world = location.getWorld();
            if (world != null) {
                worldId = world.getUID();
            }
        } catch (IllegalArgumentException e) {
            OtogiAPI.LOGGER.error("Specified world is unloaded.", e);
        }
        return new Position(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch(), worldId);
    }

    public static Position of(double x, double y, double z) {
        return new Position(x, y, z, 0, 0, null);
    }

    public static Position of(Entity e) {
        return of(e.getLocation());
    }

    public static Position of(Block block) {
        return of(block.getLocation());
    }

    @Nullable
    public World getWorld() {
        return worldId != null ? Bukkit.getWorld(worldId) : null;
    }

    public Location toLocation() {
        return new Location(getWorld(), x, y, z, yaw, pitch);
    }
}
