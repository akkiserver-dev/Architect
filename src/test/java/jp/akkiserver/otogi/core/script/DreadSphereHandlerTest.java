package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DreadSphereHandlerTest {
    private static ScriptArchitect owner() throws Exception {
        return ScriptArchitect.create(ArchitectDefinition.parse(Files.readString(
                Path.of("src/main/resources/architects/hasshaku.json"))), null);
    }

    private static DreadSphereHandler.Pending pending(AtomicBoolean cancelled) {
        var pending = new DreadSphereHandler.Pending(new Location(null, 2, 3, 4), 0, 24);
        pending.task = (BukkitTask) Proxy.newProxyInstance(BukkitTask.class.getClassLoader(), new Class[]{BukkitTask.class},
                (proxy, method, args) -> { if (method.getName().equals("cancel")) cancelled.set(true); return null; });
        return pending;
    }

    @Test void cancellationAfterLifeResetStillStopsTheCapturedTask() throws Exception {
        var owner = owner();
        var self = new ArenaPlayer(null, null);
        var stopped = new AtomicBoolean();
        var pending = pending(stopped);
        self.setState(owner.stateKey() + ".dread", pending);
        self.resetCombatState();
        DreadSphereHandler.cancel(owner, self, pending);
        assertTrue(stopped.get());
        assertFalse(self.hasState(owner.stateKey() + ".dread"));
    }

    @Test void obsoleteTaskCannotRemoveOrCancelANewCircle() throws Exception {
        var owner = owner();
        var self = new ArenaPlayer(null, null);
        var oldStopped = new AtomicBoolean();
        var newStopped = new AtomicBoolean();
        var previous = pending(oldStopped);
        var replacement = pending(newStopped);
        self.setState(owner.stateKey() + ".dread", replacement);
        DreadSphereHandler.cancel(owner, self, previous);
        assertTrue(oldStopped.get());
        assertFalse(newStopped.get());
        assertSame(replacement, self.getState(owner.stateKey() + ".dread", DreadSphereHandler.Pending.class));
        MythicSkillHandlers.clear(owner, self);
        assertTrue(newStopped.get());
        assertFalse(self.hasState(owner.stateKey() + ".dread"));
    }

    @Test void aShortSilenceNeverShortensAnExistingSeal() {
        assertEquals(130, DreadSphereHandler.silenceDeadline(null, 100, 30));
        assertEquals(130, DreadSphereHandler.silenceDeadline(90, 100, 30));
        assertEquals(200, DreadSphereHandler.silenceDeadline(200, 100, 30));
    }

    @Test void unsafeAndMisspelledParametersAreRejectedDuringLoading() throws Exception {
        assertEquals("dread_sphere", owner().definition.handler());
        assertThrows(IllegalArgumentException.class, () -> DreadSphereHandler.validate(Map.of("radius", 6.1)));
        assertThrows(IllegalArgumentException.class, () -> DreadSphereHandler.validate(Map.of("delayTicks", 8.5)));
        assertThrows(IllegalArgumentException.class, () -> DreadSphereHandler.validate(Map.of("silenceTicks", 101.0)));
        assertThrows(IllegalArgumentException.class, () -> DreadSphereHandler.validate(Map.of("radius", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> DreadSphereHandler.validate(Map.of("silenceTick", 30.0)));
        assertDoesNotThrow(() -> DreadSphereHandler.validate(Map.of("delayTicks", 8.0, "silenceTicks", 0.0)));
    }
}
