package jp.akkiserver.otogi.core.stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class MapServiceTest {
    @TempDir Path directory;
    private static final String DRAFT = "{\"id\":\"garden\",\"title\":\"庭\",\"description\":\"\",\"world\":\"flat\"}";

    @Test void emptyDirectoryIsValidAndHasNoSelectedMap() throws Exception {
        var maps = new MapService(directory);
        assertTrue(maps.getDefinitions().isEmpty());
        assertNull(maps.getDefaultMapId());
        try (var entries = Files.list(directory)) { assertEquals(0, entries.count()); }
    }
    @Test void prepareReloadDoesNotApplyUntilCommitAndMalformedReloadRetainsDefinitions() throws Exception {
        var maps = new MapService(directory);
        Files.writeString(directory.resolve("garden.json"), DRAFT);
        var prepared = maps.prepareReload();
        assertTrue(maps.getDefinitions().isEmpty());
        maps.commitReload(prepared);
        assertEquals("garden", maps.getDefaultMapId());
        Files.writeString(directory.resolve("garden.json"), "{\"id\":\"invalid/path\"}");
        assertThrows(IllegalArgumentException.class, maps::reload);
        assertEquals("garden", maps.getDefaultMapId());
        assertEquals("庭", maps.getDefinition("garden").title());
    }
    @Test void exampleFilesAreIgnoredAndJsonFilenameMustMatchId() throws Exception {
        Files.writeString(directory.resolve("map.json.example"), "example is deliberately not parsed");
        var maps = new MapService(directory);
        assertTrue(maps.getDefinitions().isEmpty());
        Files.writeString(directory.resolve("other.json"), DRAFT);
        assertThrows(IllegalArgumentException.class, maps::prepareReload);
        assertTrue(maps.getDefinitions().isEmpty());
    }
    @Test void controlRadiusSavesPerMapWithoutMovingPoint() throws Exception {
        Files.writeString(directory.resolve("garden.json"), hill("garden", 4));
        Files.writeString(directory.resolve("tower.json"), hill("tower", 7));
        var maps = new MapService(directory);
        var originalPoint = maps.getDefinition("garden").controlPoint();

        maps.setControlRadius("garden", 12.5);

        assertEquals(originalPoint, maps.getDefinition("garden").controlPoint());
        assertEquals(12.5, maps.getDefinition("garden").controlRadius());
        assertEquals(7, maps.getDefinition("tower").controlRadius());
        var reloaded = new MapService(directory);
        assertEquals(originalPoint, reloaded.getDefinition("garden").controlPoint());
        assertEquals(12.5, reloaded.getDefinition("garden").controlRadius());
        assertEquals(7, reloaded.getDefinition("tower").controlRadius());
    }
    @Test void invalidControlRadiusDoesNotChangeMemoryOrFile() throws Exception {
        Path file = directory.resolve("garden.json");
        Files.writeString(file, hill("garden", 4));
        var maps = new MapService(directory);
        String before = Files.readString(file);
        for (double invalid : new double[] {0, -1, 0.5, 65, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> maps.setControlRadius("garden", invalid));
            assertThrows(IllegalArgumentException.class, () -> maps.setPoint("garden", "point", null, invalid));
            assertEquals(4, maps.getDefinition("garden").controlRadius());
            assertEquals(before, Files.readString(file));
        }
    }
    private static String hill(String id, double radius) {
        return "{\"id\":\"" + id + "\",\"title\":\"" + id + "\",\"description\":\"\",\"world\":\"flat\","
                + "\"controlPoint\":{\"x\":10,\"y\":64,\"z\":20,\"yaw\":90,\"pitch\":0},\"controlRadius\":" + radius + "}";
    }
}
