package jp.akkiserver.otogi.core.listener;

import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real event bridge without a Minecraft world or scheduler. */
class ArenaListenerTest {
    private static final class TestArena extends Arena {
        boolean lethal;
        TestArena() {
            super(null, new Mode() {
                public NamespacedKey getId() { return new NamespacedKey("test", "mode"); }
                public int getDefaultMatchTime() { return 60; }
            });
            OtogiAPI.registerArena(this);
        }
        @Override public boolean isRunning() { return true; }
        @Override public void handleDeath(ArenaPlayer victim, ArenaPlayer killer) { lethal = true; }
        void include(ArenaPlayer player) { players.add(player); }
    }

    private static Player player(double health, double absorption) {
        UUID uuid = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getHealth" -> health;
            case "getAbsorptionAmount" -> absorption;
            case "hashCode" -> uuid.hashCode();
            case "equals" -> proxy == args[0];
            case "toString" -> "TestPlayer";
            default -> null;
        });
    }

    private static ArenaPlayer participant(TestArena arena, Player player) {
        ArenaPlayer participant = new ArenaPlayer(player, arena) {
            @Override public boolean isInvulnerable() { return false; }
        };
        arena.include(participant);
        return participant;
    }

    @SuppressWarnings("removal") // Synthetic server events only; production never constructs damage events.
    private static EntityDamageEvent hit(Player player, double finalDamage) {
        DamageSource source = (DamageSource) Proxy.newProxyInstance(DamageSource.class.getClassLoader(), new Class[]{DamageSource.class}, (proxy, method, args) -> null);
        return new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, source, finalDamage);
    }

    @AfterEach void clearArenas() { OtogiAPI.shutdown(); }

    @Test void finalDamageAlreadyAccountsForAbsorption() {
        TestArena arena = new TestArena();
        Player player = player(5, 4);
        participant(arena, player);
        EntityDamageEvent event = hit(player, 6);
        new ArenaListener().onDamage(event);
        assertTrue(event.isCancelled(), "Lethal damage must be handled by the mode, never vanilla death");
        assertTrue(arena.lethal);
    }

    @Test void cancelledDamageDoesNotTriggerConfirmedReactions() {
        TestArena arena = new TestArena();
        Player player = player(20, 0);
        ArenaPlayer victim = participant(arena, player);
        AtomicInteger confirmed = new AtomicInteger();
        victim.addArchitect(new BaseArchitect("reaction", "反応") {
            @Override public void onDamageTakenConfirmed(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) { confirmed.incrementAndGet(); }
        });
        victim.addArchitect(new BaseArchitect("guard", "防御") {
            @Override public void onDamageTaken(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) { event.setCancelled(true); }
        });
        new ArenaListener().onDamage(hit(player, 5));
        assertEquals(0, confirmed.get());
        assertFalse(arena.lethal);
    }

    @Test void originalHitCannotApplyToANewLifeCreatedByReaction() {
        TestArena arena = new TestArena();
        Player player = player(20, 0);
        ArenaPlayer victim = participant(arena, player);
        victim.addArchitect(new BaseArchitect("transition", "復帰") {
            @Override public void onDamageTakenConfirmed(ArenaPlayer self, ArenaPlayer attacker, EntityDamageEvent event) { self.resetCombatState(); }
        });
        EntityDamageEvent event = hit(player, 5);
        new ArenaListener().onDamage(event);
        assertTrue(event.isCancelled());
    }
}
