package jp.akkiserver.otogi.core.script;

import net.kyori.adventure.text.Component;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScriptArchitectFailureTest {
    private static final class Owner extends ArenaPlayer {
        int notices;
        Owner() { super(null, null); }
        @Override public void sendNotice(Component message) { notices++; }
    }

    @Test void failureDisablesOnlyOneOwnerAndCleansEffectsImmediatelyAndOnUnequip() {
        Owner failedOwner = new Owner();
        Owner healthyOwner = new Owner();
        Set<ArenaPlayer> attributes = new HashSet<>(Set.of(failedOwner, healthyOwner));
        int[] executions = {0};
        ArchitectDefinition definition = ArchitectDefinition.parse("""
                {"id":"failure_test","name":"test","description":[],"kind":"skill","material":"FEATHER",
                 "backend":"java","handler":"dash"}
                """);
        ScriptArchitect architect = new ScriptArchitect(definition, null) {
            @Override protected boolean executeHook(String hook, ArenaPlayer self, EntityDamageEvent event) {
                executions[0]++;
                if (self == failedOwner) throw new IllegalStateException("owner-specific failure");
                return true;
            }
            @Override protected void removeAttributeModifiers(ArenaPlayer self) { attributes.remove(self); }
        };
        failedOwner.setState(architect.stateKey(), "stale-script-state");
        failedOwner.setState(architect.stateKey() + ".tick", 100);
        assertFalse(architect.invoke("on_use", failedOwner, null));
        assertTrue(architect.hasFailed(failedOwner));
        assertFalse(architect.hasFailed(healthyOwner));
        assertFalse(attributes.contains(failedOwner));
        assertTrue(attributes.contains(healthyOwner));
        assertFalse(failedOwner.hasState(architect.stateKey()));
        assertFalse(failedOwner.hasState(architect.stateKey() + ".tick"));
        assertTrue(architect.invoke("on_use", healthyOwner, null));
        assertEquals(2, executions[0]);

        failedOwner.resetCombatState();
        assertFalse(architect.invoke("on_use", failedOwner, null));
        assertEquals(2, executions[0], "Disabled owners are not evaluated again after respawn");
        assertEquals(1, failedOwner.notices);
        attributes.add(failedOwner);
        failedOwner.setState(architect.stateKey(), "later-state");
        architect.onUnequip(failedOwner);
        assertFalse(attributes.contains(failedOwner), "Unequip cleanup still runs for a disabled owner");
        assertFalse(failedOwner.hasState(architect.stateKey()));
    }
}
