package jp.akkiserver.otogi.core.mode;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;

class ModeSettingsTest {
    @TempDir Path directory;
    @Test void prepareDoesNotMutateAndInvalidReplacementKeepsOldRules() throws Exception {
        var original = ModeSettings.get();
        Path file = directory.resolve("modes.json");
        Files.writeString(file, "{\"lastStand\":{\"lives\":5,\"matchSeconds\":300},\"kingOfHill\":{\"targetPoints\":120,\"matchSeconds\":900}}");
        var prepared = ModeSettings.read(file);
        assertSame(original, ModeSettings.get());
        try {
            ModeSettings.apply(prepared);
            Files.writeString(file, "{\"lastStand\":{\"lives\":0,\"matchSeconds\":300}}");
            assertThrows(IllegalArgumentException.class, () -> ModeSettings.read(file));
            assertSame(prepared, ModeSettings.get());
        } finally { ModeSettings.apply(original); }
    }
    @Test void parsesOperatorTunedRules() {
        var rules = new Gson().fromJson("{\"lastStand\":{\"lives\":5,\"matchSeconds\":300},\"kingOfHill\":{\"targetPoints\":120,\"matchSeconds\":900}}", ModeSettings.Settings.class);
        assertEquals(5, rules.lastStand().lives());
        assertEquals(120, rules.kingOfHill().targetPoints());
    }
    @Test void rejectsMissingSectionsAndUnsafeValues() {
        assertThrows(IllegalArgumentException.class, () -> new ModeSettings.Settings(null, new ModeSettings.KingOfHill(60, 600)));
        assertThrows(IllegalArgumentException.class, () -> new ModeSettings.LastStand(0, 600));
        assertThrows(IllegalArgumentException.class, () -> new ModeSettings.LastStand(3, 0));
        assertThrows(IllegalArgumentException.class, () -> new ModeSettings.KingOfHill(Integer.MAX_VALUE, 600));
        assertThrows(IllegalArgumentException.class, () -> new ModeSettings.KingOfHill(60, 7201));
    }
}
