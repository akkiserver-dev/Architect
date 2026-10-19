package jp.akkiserver.otogi;

import jp.akkiserver.otogi.core.combat.CombatSettings;
import jp.akkiserver.otogi.core.game.MatchService;
import jp.akkiserver.otogi.core.mode.ModeKingOfHill;
import jp.akkiserver.otogi.core.mode.ModeLastStand;
import jp.akkiserver.otogi.core.script.ScriptArchitectLoader;
import jp.akkiserver.otogi.core.stats.PlayerStatsService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.build.Builds;
import jp.akkiserver.otogi.api.component.I18n;
import jp.akkiserver.otogi.api.detail.PluginAPI;
import jp.akkiserver.otogi.core.architect.Architects;
import jp.akkiserver.otogi.core.command.OtogiCommand;
import jp.akkiserver.otogi.core.listener.ArenaListener;
import jp.akkiserver.otogi.core.mode.ModeFFA;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.jar.JarFile;

public final class OtogiPlugin extends JavaPlugin implements PluginAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(OtogiPlugin.class);
    private MatchService matchService;
    private PlayerStatsService playerStats;
    private ScriptArchitectLoader scriptArchitects;
    private jp.akkiserver.otogi.core.lobby.LobbyMenu lobbyMenu;
    private jp.akkiserver.otogi.core.lobby.LobbyCompass lobbyCompass;
    private org.bukkit.configuration.file.FileConfiguration activeConfig;

    @Override public org.bukkit.configuration.file.FileConfiguration getConfig() {
        return activeConfig == null ? super.getConfig() : activeConfig;
    }

    public MatchService getMatchService() { return matchService; }
    public PlayerStatsService getPlayerStats() { return playerStats; }
    public jp.akkiserver.otogi.core.lobby.LobbyMenu getLobbyMenu() { return lobbyMenu; }
    public int validateArchitects() {
        try { return scriptArchitects.prepareReload().size(); }
        catch (IOException exception) { throw new IllegalArgumentException(exception.getMessage(), exception); }
    }

    @Override
    public void onLoad() {
        OtogiAPI.plugin = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig(); //force regenerate plugin directory and config before load
        activeConfig = super.getConfig();

        if (!getConfig().getBoolean("enabled", true)) {
            LOGGER.warn("architect not enabled in config. disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveResourceFolder("lang");

        I18n.init();

        CombatSettings.load(this);

        Architects.registerDefaults();
        saveResourceFolder("architects");
        scriptArchitects = new ScriptArchitectLoader(getDataFolder().toPath().resolve("architects"));
        var scripts = scriptArchitects.reload();
        if (!scripts.success()) throw new IllegalStateException(scripts.message());
        Builds.load(new File(getDataFolder(), "builds.yml"));
        OtogiAPI.registerMode(new ModeFFA());
        OtogiAPI.registerMode(new ModeLastStand());
        OtogiAPI.registerMode(new ModeKingOfHill());
        playerStats = new PlayerStatsService(this);
        matchService = new MatchService(this);
        registerEventListener(new ArenaListener());
        lobbyMenu = new jp.akkiserver.otogi.core.lobby.LobbyMenu(this, matchService, playerStats);
        lobbyCompass = new jp.akkiserver.otogi.core.lobby.LobbyCompass(this, lobbyMenu);

        OtogiCommand command = new OtogiCommand(this);
        getCommand("otogi").setExecutor(command);
        getCommand("otogi").setTabCompleter(command);
    }

    @Override
    public void onDisable() {
        OtogiAPI.shutdown();
        if (lobbyCompass != null) lobbyCompass.close();
        Builds.save();
        if (playerStats != null) playerStats.save();
        I18n.shutdown();
    }

    public void reloadSettings() {
        if (matchService != null && matchService.getActiveArena() != null)
            throw new IllegalStateException("試合終了後に再読み込みしてください。");
        // Read/validate every source before publishing any runtime changes.
        try {
            var config = new org.bukkit.configuration.file.YamlConfiguration();
            var file = new File(getDataFolder(), "config.yml");
            if (file.exists()) config.load(file);
            try (var resource = getResource("config.yml")) {
                if (resource != null) config.setDefaults(org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8)));
            }
            var combat = CombatSettings.prepare(this);
            var translations = I18n.prepare();
            var scripts = scriptArchitects.prepareReload();
            var match = matchService.prepareReload();
            // Registry conflicts can still be rejected here, before the other snapshots are published.
            scriptArchitects.commitReload(scripts);
            matchService.commitReload(match);
            CombatSettings.commit(combat);
            I18n.commit(translations);
            activeConfig = config;
            Builds.revalidateAll();
            for (var player : Bukkit.getOnlinePlayers()) {
                var gui = de.themoep.inventorygui.InventoryGui.getOpen(player);
                if (gui != null && gui.getPlugin() == this) gui.close(player, false);
            }
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
            throw new IllegalArgumentException("設定を読み込めません。以前の設定を維持します: " + exception.getMessage(), exception);
        }
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
           connection.setUseCaches(false);

           try (JarFile jar = connection.getJarFile()) {
               String prefix = folder.endsWith("/") ? folder : folder + "/";

               jar.stream()
                       .filter(entry -> entry.getName().startsWith(prefix))
                       .filter(entry -> !entry.isDirectory())
                       .forEach(entry -> {
                           try (InputStream in = jar.getInputStream(entry)) {
                               File out = new File(getDataFolder(), entry.getName());

                               out.getParentFile().mkdirs();

                               if (!out.exists() && !(folder.equals("architects")
                                       && ScriptArchitectLoader.hasLegacyDefault(out.toPath()))) {
                                   Files.copy(in, out.toPath());
                               }
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
