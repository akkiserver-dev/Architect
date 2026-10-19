package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MythicSkillHandlersTest {
    private static ScriptArchitect architect() {
        return ScriptArchitect.create(ArchitectDefinition.parse("""
                {"id":"thunder_test","name":"test","description":[],"kind":"skill","material":"FEATHER",
                 "backend":"java","handler":"thunder"}
                """), null);
    }

    private static MythicSkillHandlers.PendingStrike pending(AtomicBoolean cancelled) {
        var strike = new MythicSkillHandlers.PendingStrike(new Location(null, 0, 0, 0), 12, 0, 20);
        strike.task = (BukkitTask) Proxy.newProxyInstance(BukkitTask.class.getClassLoader(), new Class[]{BukkitTask.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("cancel")) cancelled.set(true);
                    return null;
                });
        return strike;
    }

    @Test void lifeResetCannotLeaveTheCapturedStrikeTaskRunning() {
        var owner = architect();
        var self = new ArenaPlayer(null, null);
        var cancelled = new AtomicBoolean();
        var strike = pending(cancelled);
        self.setState(owner.stateKey() + ".mythic", strike);
        self.resetCombatState();
        MythicSkillHandlers.cancelStrike(owner, self, strike);
        assertTrue(cancelled.get());
        assertFalse(self.hasState(owner.stateKey() + ".mythic"));
    }

    @Test void obsoleteTaskCancellationPreservesAReplacementStrike() {
        var owner = architect();
        var self = new ArenaPlayer(null, null);
        var cancelled = new AtomicBoolean();
        var oldStrike = pending(cancelled);
        var replacementCancelled = new AtomicBoolean();
        var replacement = pending(replacementCancelled);
        self.setState(owner.stateKey() + ".mythic", replacement);
        MythicSkillHandlers.cancelStrike(owner, self, oldStrike);
        assertTrue(cancelled.get());
        assertSame(replacement, self.getState(owner.stateKey() + ".mythic", MythicSkillHandlers.PendingStrike.class));
        assertFalse(replacementCancelled.get());
        MythicSkillHandlers.clear(owner, self);
        assertTrue(replacementCancelled.get());
        assertFalse(self.hasState(owner.stateKey() + ".mythic"));
    }

    @Test void mythicParametersRejectTyposFractionsAndUnboundedEffects() {
        assertThrows(IllegalArgumentException.class, () -> MythicSkillHandlers.validate("thunder", Map.of("delayTicks", 4.5)));
        assertThrows(IllegalArgumentException.class, () -> MythicSkillHandlers.validate("thunder", Map.of("width", 3.0)));
        assertThrows(IllegalArgumentException.class, () -> MythicSkillHandlers.validate("thunder", Map.of("delayTick", 20.0)));
        assertThrows(IllegalArgumentException.class, () -> MythicSkillHandlers.validate("frost_cone", Map.of("slowTicks", 201.0)));
        assertThrows(IllegalArgumentException.class, () -> MythicSkillHandlers.validate("oni_frenzy", Map.of("toolMultiplier", Double.NaN)));
        assertDoesNotThrow(() -> MythicSkillHandlers.validate("thunder", Map.of("delayTicks", 4.0, "width", 2.0)));
    }
}
