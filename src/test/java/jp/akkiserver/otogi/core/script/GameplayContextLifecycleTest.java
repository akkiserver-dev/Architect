package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.mode.ModeFFA;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GameplayContextLifecycleTest {
    private static final class TestArena extends Arena {
        boolean running = true;
        TestArena() { super(null, new ModeFFA()); }
        @Override public boolean isRunning() { return running; }
        void include(ArenaPlayer self) { players.add(self); }
        void leave(ArenaPlayer self) { players.remove(self); }
    }

    private record Fixture(TestArena arena, ArenaPlayer self, GameplayContext context) {}

    private static Fixture fixture() {
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isOnline" -> true;
                    case "isDead" -> false;
                    default -> null;
                });
        var arena = new TestArena();
        var self = new ArenaPlayer(player, arena);
        var owner = ScriptArchitect.create(ArchitectDefinition.parse("""
                {"id":"life_test","name":"test","description":[],"kind":"skill","material":"FEATHER",
                 "backend":"java","handler":"dash"}
                """), null);
        self.addArchitect(owner);
        arena.include(self);
        return new Fixture(arena, self, new GameplayContext(owner, self, "on_use", null));
    }

    @Test void anOldHookCannotContinueIntoANewLife() {
        var fixture = fixture();
        assertTrue(fixture.context.valid());
        fixture.self.resetCombatState();
        assertFalse(fixture.context.valid());
    }

    @Test void endedMatchesLeaversAndEliminatedPlayersCannotApplyEffects() {
        var fixture = fixture();
        fixture.arena.running = false;
        assertFalse(fixture.context.valid());
        fixture.arena.running = true;
        fixture.arena.leave(fixture.self);
        assertFalse(fixture.context.valid());
        fixture.arena.include(fixture.self);
        fixture.self.setEliminated(true);
        assertFalse(fixture.context.valid());
    }
}
