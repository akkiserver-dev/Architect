package jp.akkiserver.otogi.api.arena;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.api.arena.mode.Mode;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class ArenaDeathTest {
    private static final class TestArena extends Arena {
        TestArena(Mode mode) { super(null, mode); }
        @Override public boolean isRunning() { return true; }
        ArenaPlayer include(String name) {
            UUID id = UUID.randomUUID();
            Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "getUniqueId" -> id;
                        case "getName" -> name;
                        case "hashCode" -> id.hashCode();
                        case "equals" -> proxy == args[0];
                        default -> null;
                    });
            ArenaPlayer participant = new ArenaPlayer(player, this);
            players.add(participant);
            return participant;
        }
        void depart(ArenaPlayer player) { players.remove(player); }
    }

    @Test void departedAttackersCannotEarnEnvironmentalKillsOrWinTheMatch() {
        AtomicReference<ArenaPlayer> credited = new AtomicReference<>();
        TestArena arena = new TestArena(new Mode() {
            @Override public NamespacedKey getId() { return new NamespacedKey("test", "death"); }
            @Override public int getDefaultMatchTime() { return 60; }
            @Override public void onDeath(Arena match, ArenaPlayer victim, ArenaPlayer killer) { credited.set(killer); }
        });
        ArenaPlayer attacker = arena.include("attacker"), victim = arena.include("victim");
        arena.depart(attacker);
        arena.handleDeath(victim, attacker);
        assertNull(credited.get());
        assertEquals(0, attacker.getKills());
        assertEquals(1, victim.getDeaths());
    }

    @Test void eliminatedPlayersCannotBeKilledOrCreditedForANewKill() {
        TestArena arena = new TestArena(new Mode() {
            @Override public NamespacedKey getId() { return new NamespacedKey("test", "elimination"); }
            @Override public int getDefaultMatchTime() { return 60; }
        });
        ArenaPlayer attacker = arena.include("attacker"), victim = arena.include("victim");
        attacker.setEliminated(true);
        arena.handleDeath(victim, attacker);
        assertEquals(0, attacker.getKills());
        victim.setEliminated(true);
        arena.handleDeath(victim, null);
        assertEquals(1, victim.getDeaths());
    }
}
