package jp.akkiserver.otogi.core.combat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reloadable combat tuning, separate from arena and build configuration. */
public final class CombatSettings {
    private static YamlConfiguration values = new YamlConfiguration();

    public static void load(Plugin plugin) {
        commit(prepare(plugin));
    }

    public static YamlConfiguration prepare(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "combat.yml");
        if (!file.exists()) plugin.saveResource("combat.yml", false);
        YamlConfiguration loaded = new YamlConfiguration();
        try { loaded.load(file); }
        catch (java.io.IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new IllegalArgumentException("combat.yml を読み込めません: " + exception.getMessage(), exception);
        }
        try (var stream = plugin.getResource("combat.yml")) {
            if (stream != null) loaded.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        } catch (java.io.IOException ex) {
            plugin.getLogger().warning("combat.yml の既定値を読み込めませんでした: " + ex.getMessage());
        }
        return loaded;
    }

    public static void commit(YamlConfiguration prepared) { values = prepared; }

    public static double number(String path, double fallback, double min, double max) {
        double value = values.getDouble(path, fallback);
        return CombatGeometry.clampFinite(value, fallback, min, max);
    }

    public static int ticks(String path, int fallback) {
        return (int) number(path, fallback, 1, 12000);
    }

    public static boolean enabled(String path) { return values.getBoolean(path, true); }

    private CombatSettings() {}
}
