package jp.akkiserver.otogi.core.stats;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.api.stage.Stage;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerStatsServiceTest {
    @TempDir Path tempDir;

    @Test
    void recordsParticipantsAndWinnersOnceAcrossReload() {
        Path file = tempDir.resolve("player-stats.json");
        UUID matchId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        UUID quitterId = UUID.randomUUID();
        Arena arena = endedArena(matchId, winnerId, quitterId);
        PlayerStatsService service = new PlayerStatsService(file);

        assertTrue(service.recordMatch(arena));
        assertEquals(new PlayerStats(4, 1, 1, 1), service.get(winnerId));
        assertEquals(new PlayerStats(1, 2, 1, 0), service.get(quitterId));

        PlayerStatsService reloaded = new PlayerStatsService(file);
        assertFalse(reloaded.recordMatch(endedArena(matchId, winnerId, quitterId)));
        assertEquals(new PlayerStats(4, 1, 1, 1), reloaded.get(winnerId));
        assertEquals(new PlayerStats(1, 2, 1, 0), reloaded.get(quitterId));
    }

    private static Arena endedArena(UUID matchId, UUID winnerId, UUID quitterId) {
        Mode mode = new Mode() {
            @Override public NamespacedKey getId() { return new NamespacedKey("otogi", "test"); }
            @Override public int getDefaultMatchTime() { return 60; }
            @Override public Set<UUID> getWinners(Arena arena) { return Set.of(winnerId); }
        };
        return new Arena((Stage) null, mode) {
            private final Set<ArenaPlayer> participants = Set.of(
                    playerResult(winnerId, 4, 1, this),
                    playerResult(quitterId, 1, 2, this));

            @Override public boolean isEnded() { return true; }
            @Override public UUID getMatchId() { return matchId; }
            @Override public Set<ArenaPlayer> getParticipants() { return participants; }
        };
    }

    private static ArenaPlayer playerResult(UUID id, int kills, int deaths, Arena arena) {
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> method.getName().equals("getUniqueId") ? id : null);
        ArenaPlayer participant = new ArenaPlayer(player, arena);
        for (int i = 0; i < kills; i++) participant.addKill();
        for (int i = 0; i < deaths; i++) participant.addDeath();
        return participant;
    }
}
