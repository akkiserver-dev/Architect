package jp.akkiserver.otogi.core.stage;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MapDefinitionTest {
    @Test void incompleteDraftSurvivesJsonRoundTrip() {
        var draft = new MapDefinition("garden", "幽世の庭", "", "flat", null, List.of(), null, null, null, 5, null, null);
        var gson = new Gson();
        var loaded = gson.fromJson(gson.toJson(draft), MapDefinition.class);
        assertEquals(draft, loaded);
        assertTrue(loaded.spawns().isEmpty());
        assertNull(loaded.min());
        assertEquals("flat", loaded.world());
    }
    @Test void bundledExampleReferencesExistingWorldAndHasCompletePositions() {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream("/maps/map.json.example"), StandardCharsets.UTF_8)) {
            var map = new Gson().fromJson(reader, MapDefinition.class);
            assertEquals("world", map.world());
            assertTrue(map.spawns().size() >= 2);
            assertNotNull(map.spectator());
            assertNotNull(map.recovery());
            assertNotNull(map.min());
            assertNotNull(map.max());
            assertNotNull(map.controlPoint());
        } catch (java.io.IOException exception) { fail(exception); }
    }
    @Test void rejectsPathTraversalIdsAndNonFiniteCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new MapDefinition("../world", "x", "", "world", null, List.of(), null, null, null, 5, null, null));
        assertThrows(IllegalArgumentException.class, () -> new MapDefinition.Point(Double.NaN, 65, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MapDefinition.Point(0, 65, 0, Float.POSITIVE_INFINITY, 0));
    }
    @Test void customMapHasNoArtificialTerrainSizeLimit() {
        var map = new MapDefinition("large", "広いマップ", "", "world", null, List.of(), null, null, null, 10,
                new MapDefinition.Point(-1000, -60, -1000, 0, 0), new MapDefinition.Point(1000, 300, 1000, 0, 0));
        assertEquals(1000, map.max().x());
    }
}
