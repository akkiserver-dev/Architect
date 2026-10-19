package jp.akkiserver.otogi.core.mode;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.arena.mode.Mode;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ModeWinnersTest {
    private static final class Match extends Arena {
        Match(Mode mode) { super(null, mode); }
        ArenaPlayer include(String name, int kills, int deaths) {
            UUID uuid = UUID.randomUUID();
            Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getUniqueId" -> uuid;
                        case "getName" -> name;
                        case "hashCode" -> uuid.hashCode();
                        case "equals" -> proxy == args[0];
                        default -> null;
                    });
            ArenaPlayer participant = new ArenaPlayer(player, this);
            for (int i = 0; i < kills; i++) participant.addKill();
            for (int i = 0; i < deaths; i++) participant.addDeath();
            participant.setMatchState("mode.last_stand.lives", 3);
            players.add(participant);
            return participant;
        }
    }

    @Test void lastStandPrioritizesRemainingLivesOverKills() {
        Mode mode = new ModeLastStand();
        Match match = new Match(mode);
        match.include("kills", 10, 2);
        ArenaPlayer survivor = match.include("survivor", 0, 1);
        assertEquals(Set.of(survivor.getPlayer().getUniqueId()), mode.getWinners(match));
        assertSame(survivor, mode.getRanking(match).getFirst());
    }

    @Test void spectatorsCannotWinWhenAllSurvivorsHaveLeft() {
        Mode mode = new ModeLastStand();
        Match match = new Match(mode);
        match.include("eliminated", 10, 3).setEliminated(true);
        assertTrue(mode.getWinners(match).isEmpty());
    }

    @Test void kingOfHillUsesControlPointsAndTreatsEqualPointsAsADraw() {
        Mode mode = new ModeKingOfHill();
        Match match = new Match(mode);
        ArenaPlayer fighter = match.include("fighter", 10, 0);
        ArenaPlayer controller = match.include("controller", 0, 2);
        fighter.setMatchState("mode.king_of_hill.score", 5);
        controller.setMatchState("mode.king_of_hill.score", 10);
        assertEquals(Set.of(controller.getPlayer().getUniqueId()), mode.getWinners(match));
        assertSame(controller, mode.getRanking(match).getFirst());
        fighter.setMatchState("mode.king_of_hill.score", 10);
        assertTrue(mode.getWinners(match).isEmpty());
    }
}
