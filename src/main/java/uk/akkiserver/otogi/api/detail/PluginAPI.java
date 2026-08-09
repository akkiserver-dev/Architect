package uk.akkiserver.otogi.api.detail;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public interface PluginAPI {
    Plugin getPlugin();

    default void registerEventListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, getPlugin());
    }

    FileConfiguration getConfig();
}
