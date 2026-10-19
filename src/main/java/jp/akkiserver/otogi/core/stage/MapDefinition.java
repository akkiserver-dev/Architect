package jp.akkiserver.otogi.core.stage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import jp.akkiserver.otogi.api.util.Position;
import java.util.List;
import java.util.UUID;

/** Persisted metadata only. A draft may omit positions until the operator completes setup. */
public record MapDefinition(String id, String title, String description, String world, UUID worldId,
                            List<Point> spawns, Point spectator, Point recovery, Point controlPoint,
                            double controlRadius, Point min, Point max) {
    public MapDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}")) throw new IllegalArgumentException("IDは英小文字で始まる英数字と_、48文字以内です。");
        if (title == null || title.isBlank() || description == null) throw new IllegalArgumentException("titleとdescriptionが必要です。");
        if (world == null || world.isBlank()) throw new IllegalArgumentException("既存ワールド名worldが必要です。");
        spawns = spawns == null ? List.of() : List.copyOf(spawns);
        if (spawns.size() > 128) throw new IllegalArgumentException("spawnは128点まで登録できます。");
        if (controlRadius == 0) controlRadius = 5;
        if (!Double.isFinite(controlRadius) || controlRadius < 1 || controlRadius > 64) throw new IllegalArgumentException("拠点半径は1〜64です。");
    }
    /** UUID, when supplied, prevents accidentally selecting a replacement world with the same name. */
    public World resolveWorld() { return worldId == null ? Bukkit.getWorld(world) : Bukkit.getWorld(worldId); }

    public record Point(double x, double y, double z, float yaw, float pitch) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch))
                throw new IllegalArgumentException("座標は有限の数値で指定してください。");
            if (Math.abs(x) > 29_999_984 || Math.abs(z) > 29_999_984)
                throw new IllegalArgumentException("座標がMinecraftのワールド範囲外です。");
        }
        public static Point of(Location location) { return new Point(location.x(), location.y(), location.z(), location.getYaw(), location.getPitch()); }
        public Position in(World world) { return new Position(x, y, z, yaw, pitch, world.getUID()); }
    }
}
