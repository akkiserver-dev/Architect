package jp.akkiserver.otogi.core.game;

import jp.akkiserver.otogi.core.mode.ModeKingOfHill;
import jp.akkiserver.otogi.core.mode.ModeSettings;
import jp.akkiserver.otogi.core.stage.MapDefinition;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.stage.MapService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/** One shared match and waiting list for commands and lobby menus. Server-thread only. */
public final class MatchService {
    private final JavaPlugin plugin;
    private final MapService maps;
    private final LinkedHashSet<UUID> queue = new LinkedHashSet<>();
    private Arena active;
    private String selectedMap;
    private String selectedMode = "otogi:ffa";
    public MatchService(JavaPlugin plugin) {
        this.plugin = plugin;
        ModeSettings.load(plugin);
        maps = new MapService(plugin);
        selectedMap = maps.getDefaultMapId();
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::enforceBounds, 5, 5);
    }
    public MapService getMaps() { return maps; }
    public Arena getActiveArena() {
        if (active != null && active.isEnded()) active = null;
        return active;
    }
    public String getSelectedMapId() { return selectedMap; }
    public String getSelectedModeId() { return selectedMode; }
    public void selectMap(String id) {
        if (maps.getDefinition(id) == null) throw new IllegalArgumentException("マップが見つかりません: " + id);
        selectedMap = id;
    }
    public void selectMode(String id) {
        NamespacedKey key = key(id);
        if (key == null || OtogiAPI.getMode(key) == null) throw new IllegalArgumentException("モードが見つかりません: " + id);
        selectedMode = key.toString();
    }
    public boolean join(Player player) {
        if (getActiveArena() != null || OtogiAPI.getArenaPlayer(player) != null || player.getGameMode() == GameMode.SPECTATOR) return false;
        return queue.add(player.getUniqueId());
    }
    public boolean leave(Player player) {
        if (queue.remove(player.getUniqueId())) return true;
        Arena current = getActiveArena();
        return current != null && current.removePlayer(player);
    }
    public Collection<Player> getQueuedPlayers() {
        queue.removeIf(id -> Bukkit.getPlayer(id) == null);
        return queue.stream().map(Bukkit::getPlayer).filter(java.util.Objects::nonNull).toList();
    }
    public Arena start(String modeId, String mapId, Collection<? extends Player> participants) {
        if (getActiveArena() != null) throw new IllegalStateException("試合は既に進行中です。");
        NamespacedKey key = key(modeId);
        var mode = key == null ? null : OtogiAPI.getMode(key);
        if (mode == null) throw new IllegalArgumentException("モードが見つかりません: " + modeId);
        List<? extends Player> players = participants.stream().filter(Player::isOnline).distinct().toList();
        if (players.size() < 2) throw new IllegalArgumentException("参加待機中のプレイヤーが2人以上必要です。");
        if (players.stream().anyMatch(player -> OtogiAPI.getArenaPlayer(player) != null || player.getGameMode() == GameMode.SPECTATOR))
            throw new IllegalArgumentException("既に参加中または観戦モードのプレイヤーが含まれています。");
        String selected = mapId == null ? selectedMap : mapId;
        var stage = maps.prepare(selected);
        if (mode instanceof ModeKingOfHill && maps.getDefinition(selected).controlPoint() == null)
            throw new IllegalArgumentException("拠点pointを設定してください: /otogi map point " + selected + " [半径]");
        if (players.size() > stage.getSpawnPositions().size()) throw new IllegalArgumentException("このマップの定員は " + stage.getSpawnPositions().size() + " 人です。");
        Arena arena = new Arena(stage, mode);
        for (Player player : players) {
            if (!arena.addPlayer(player)) throw new IllegalStateException("参加できませんでした: " + player.getName());
        }
        active = arena;
        try {
            arena.start();
            queue.removeAll(players.stream().map(Player::getUniqueId).toList());
            selectedMap = stage.getId();
            selectedMode = mode.getId().toString();
            return arena;
        } catch (RuntimeException exception) {
            arena.end(false);
            active = null;
            throw exception;
        }
    }
    public boolean stop() {
        Arena arena = getActiveArena();
        if (arena == null) return false;
        arena.end(false);
        active = null;
        return true;
    }
    public void reload() {
        commitReload(prepareReload());
    }
    public record ReloadSnapshot(ModeSettings.Settings modes,
                                 java.util.Map<String, MapDefinition> maps) {}
    public ReloadSnapshot prepareReload() {
        if (getActiveArena() != null) throw new IllegalStateException("試合終了後にマップを再読込してください。");
        return new ReloadSnapshot(ModeSettings.read(plugin), maps.prepareReload());
    }
    public void commitReload(ReloadSnapshot snapshot) {
        ModeSettings.apply(snapshot.modes());
        maps.commitReload(snapshot.maps());
        if (maps.getDefinition(selectedMap) == null) selectedMap = maps.getDefaultMapId();
    }
    private void enforceBounds() {
        Arena arena = getActiveArena();
        if (arena == null || !arena.isRunning()) return;
        for (ArenaPlayer player : List.copyOf(arena.getPlayers())) {
            if (!player.getPlayer().isOnline()) { arena.removePlayer(player.getPlayer()); continue; }
            if (arena.getStage().contains(player.getPlayer().getLocation())) continue;
            if (player.isEliminated()) {
                if (!player.teleport(arena.getStage().getSpectatorSpawn())) arena.end(false);
            }
            else arena.handleDeath(player, player.getRecentAttacker());
            if (!arena.isRunning()) break;
        }
    }
    private static NamespacedKey key(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase(java.util.Locale.ROOT);
        return NamespacedKey.fromString(normalized.contains(":") ? normalized : "otogi:" + normalized);
    }
}
