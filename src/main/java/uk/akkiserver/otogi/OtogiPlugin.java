package uk.akkiserver.otogi;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.akkiserver.otogi.api.OtogiAPI;
import uk.akkiserver.otogi.api.component.I18n;
import uk.akkiserver.otogi.api.detail.PluginAPI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

public final class OtogiPlugin extends JavaPlugin implements PluginAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(OtogiPlugin.class);

    @Override
    public void onLoad() {
        OtogiAPI.plugin = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig(); //force regenerate plugin directory and config before load

        if (!getConfig().getBoolean("enabled", true)) {
            LOGGER.warn("architect not enabled in config. disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveResourceFolder("lang");

        I18n.init();
    }

    @Override
    public void onDisable() {

    }

    public void saveResourceFolder(String folder) {
        URL url = getClassLoader().getResource(folder);

        if (url == null) {
            throw new IllegalArgumentException("Folder not found: " + folder);
        }

        if (!url.getProtocol().equals("jar")) {
            throw new UnsupportedOperationException("Only jar resources supported");
        }

       try {
           JarURLConnection connection = (JarURLConnection) url.openConnection();

           try (JarFile jar = connection.getJarFile()) {
               String prefix = folder.endsWith("/") ? folder : folder + "/";

               jar.stream()
                       .filter(entry -> entry.getName().startsWith(prefix))
                       .filter(entry -> !entry.isDirectory())
                       .forEach(entry -> {
                           try (InputStream in = jar.getInputStream(entry)) {
                               File out = new File(getDataFolder(), entry.getName());

                               out.getParentFile().mkdirs();

                               Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
                           } catch (IOException e) {
                               throw new RuntimeException(e);
                           }
                       });
           }
       } catch (IOException e) {
           LOGGER.error("Unable to save resource folder: {}", folder, e);
       }
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }
}
