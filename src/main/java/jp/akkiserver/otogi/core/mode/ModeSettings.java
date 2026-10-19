package jp.akkiserver.otogi.core.mode;

import com.google.gson.Gson;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Validated JSON values. Active matches snapshot these values so reload cannot change their rules. */
public final class ModeSettings {
    public record LastStand(int lives, int matchSeconds) {
        public LastStand {
            if (lives < 1 || lives > 100 || matchSeconds < 30 || matchSeconds > 7200)
                throw new IllegalArgumentException("lastStand: livesは1〜100、matchSecondsは30〜7200です。");
        }
    }
    public record KingOfHill(int targetPoints, int matchSeconds) {
        public KingOfHill {
            if (targetPoints < 1 || targetPoints > 7200 || matchSeconds < 30 || matchSeconds > 7200)
                throw new IllegalArgumentException("kingOfHill: targetPointsは1〜7200、matchSecondsは30〜7200です。");
        }
    }
    public record Settings(LastStand lastStand, KingOfHill kingOfHill) {
        public Settings {
            if (lastStand == null || kingOfHill == null) throw new IllegalArgumentException("lastStandとkingOfHillの設定が必要です。");
        }
    }
    private static volatile Settings settings = new Settings(new LastStand(3, 600), new KingOfHill(60, 600));
    private ModeSettings() {}
    public static Settings get() { return settings; }
    public static void load(JavaPlugin plugin) {
        apply(read(plugin));
    }
    public static void apply(Settings replacement) { settings = java.util.Objects.requireNonNull(replacement); }
    public static Settings read(JavaPlugin plugin) {
        Path file = plugin.getDataFolder().toPath().resolve("modes.json");
        if (!Files.exists(file)) plugin.saveResource("modes.json", false);
        return read(file);
    }
    static Settings read(Path file) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Settings loaded = new Gson().fromJson(reader, Settings.class);
            if (loaded == null) throw new IllegalArgumentException("modes.jsonは空にできません。");
            return loaded;
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("modes.jsonを読み込めません。以前の設定を維持します: " + exception.getMessage(), exception);
        }
    }
}
