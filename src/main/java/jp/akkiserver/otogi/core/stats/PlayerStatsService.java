package jp.akkiserver.otogi.core.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Stores lifetime match statistics in plugins/Otogi/player-stats.json. */
public final class PlayerStatsService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final Map<UUID, MutableStats> players = new HashMap<>();
    private final Set<UUID> recordedMatches = new HashSet<>();

    public PlayerStatsService(@NotNull JavaPlugin plugin) {
        this(plugin.getDataFolder().toPath().resolve("player-stats.json"));
    }

    public PlayerStatsService(@NotNull Path file) {
        this.file = file;
        load();
    }

    public synchronized @NotNull PlayerStats get(@NotNull UUID playerId) {
        MutableStats stats = players.get(playerId);
        return stats == null ? new PlayerStats(0, 0, 0, 0) : stats.snapshot();
    }

    public @NotNull PlayerStats get(@NotNull Player player) {
        return get(player.getUniqueId());
    }

    /**
     * Records an ended arena once. The arena supplies a stable match ID and its mode supplies winners.
     * @return true if this call recorded the match, false if it was already recorded or is still active
     */
    public synchronized boolean recordMatch(@NotNull Arena arena) {
        if (!arena.isEnded()) {
            return false;
        }
        UUID matchId = arena.getMatchId();
        if (recordedMatches.contains(matchId)) {
            return false;
        }
        Set<UUID> winners = arena.getMode().getWinners(arena);
        Map<UUID, MutableStats> touched = new HashMap<>();
        for (ArenaPlayer arenaPlayer : arena.getParticipants()) {
            UUID playerId = arenaPlayer.getPlayer().getUniqueId();
            MutableStats stats = players.computeIfAbsent(playerId, ignored -> new MutableStats());
            touched.put(playerId, stats);
            stats.kills += arenaPlayer.getKills();
            stats.deaths += arenaPlayer.getDeaths();
            stats.matches++;
            if (winners.contains(playerId)) {
                stats.wins++;
            }
        }
        recordedMatches.add(matchId);
        try {
            save();
        } catch (RuntimeException exception) {
            recordedMatches.remove(matchId);
            for (ArenaPlayer arenaPlayer : arena.getParticipants()) {
                UUID playerId = arenaPlayer.getPlayer().getUniqueId();
                MutableStats value = touched.get(playerId);
                if (value == null) continue;
                value.kills -= arenaPlayer.getKills();
                value.deaths -= arenaPlayer.getDeaths();
                value.matches--;
                if (winners.contains(playerId)) value.wins--;
                if (value.kills == 0 && value.deaths == 0 && value.matches == 0 && value.wins == 0) players.remove(playerId);
            }
            throw exception;
        }
        return true;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            SaveData data = new SaveData();
            data.version = 1;
            data.players = new HashMap<>();
            players.forEach((id, stats) -> data.players.put(id.toString(), stats));
            data.recordedMatches = recordedMatches.stream().map(UUID::toString).toList();
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save player stats to " + file, exception);
        }
    }

    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            SaveData data = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), SaveData.class);
            if (data == null) return;
            if (data.players != null) {
                data.players.forEach((id, stats) -> {
                    try {
                        players.put(UUID.fromString(id), stats == null ? new MutableStats() : stats);
                    } catch (IllegalArgumentException ignored) {
                        // Skip malformed player IDs while preserving the rest of the file.
                    }
                });
            }
            if (data.recordedMatches != null) {
                for (String id : data.recordedMatches) {
                    try {
                        recordedMatches.add(UUID.fromString(id));
                    } catch (IllegalArgumentException ignored) {
                        // Skip malformed IDs.
                    }
                }
            }
        } catch (IOException | JsonParseException exception) {
            throw new IllegalStateException("Unable to load player stats from " + file, exception);
        }
    }

    private static final class SaveData {
        int version;
        Map<String, MutableStats> players;
        java.util.List<String> recordedMatches;
    }

    private static final class MutableStats {
        int kills;
        int deaths;
        int matches;
        int wins;

        PlayerStats snapshot() {
            return new PlayerStats(kills, deaths, matches, wins);
        }
    }
}
