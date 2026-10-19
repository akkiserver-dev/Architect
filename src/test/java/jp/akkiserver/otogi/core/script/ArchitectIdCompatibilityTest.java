package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.build.Builds;
import jp.akkiserver.otogi.api.util.ArchitectIds;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectIdCompatibilityTest {
    @TempDir Path directory;
    @AfterEach void clear() { OtogiAPI.shutdown(); }

    private static String definition(String id) {
        return """
                {"id":"%s","name":"edited","description":[],"kind":"skill","material":"FEATHER",
                 "backend":"java","handler":"pulse","parameters":{"damage":9}}
                """.formatted(id);
    }

    @Test void legacyFilesKeepCustomValuesAndResolveToOneCanonicalArchitect() throws Exception {
        Files.writeString(directory.resolve("script_nova.json"), definition("script_nova"));
        var loader = new ScriptArchitectLoader(directory);
        assertTrue(loader.reload().success());
        var current = (ScriptArchitect) OtogiAPI.getArchitect("otogi:nova");
        assertNotNull(current);
        assertEquals(9, current.definition.parameter("damage", 4));
        assertSame(current, OtogiAPI.getArchitect("otogi:script_nova"));
        assertSame(current, OtogiAPI.getArchitect(new NamespacedKey("otogi", "script_nova")));
        assertEquals(1, OtogiAPI.getArchitects().size());
        assertTrue(ScriptArchitectLoader.hasLegacyDefault(directory.resolve("nova.json")));
        assertTrue(Files.readString(directory.resolve("script_nova.json")).contains("script_nova"), "Operator files are preserved");
    }

    @Test void existingSavedBuildsLoadAndSaveUsingTheNewId() throws Exception {
        Files.writeString(directory.resolve("nova.json"), definition("nova"));
        assertTrue(new ScriptArchitectLoader(directory).reload().success());
        UUID player = UUID.randomUUID();
        Path file = directory.resolve("builds.yml");
        Files.writeString(file, player + ":\n- otogi:script_nova\n");
        Builds.load(file.toFile());
        assertEquals(1, Builds.get(player).size());
        assertEquals("nova", Builds.get(player).getArchitects().getFirst().getId().getKey());
        Builds.save();
        String saved = Files.readString(file);
        assertTrue(saved.contains("otogi:nova"));
        assertFalse(saved.contains("script_nova"));
    }

    @Test void duplicateLegacyAndNewDefinitionsRejectTheBatchWithoutReplacingLiveState() throws Exception {
        Files.writeString(directory.resolve("nova.json"), definition("nova"));
        var loader = new ScriptArchitectLoader(directory);
        assertTrue(loader.reload().success());
        var previous = OtogiAPI.getArchitect("otogi:nova");
        Files.writeString(directory.resolve("script_nova.json"), definition("script_nova"));
        assertFalse(loader.reload().success());
        assertSame(previous, OtogiAPI.getArchitect("otogi:nova"));
    }

    @Test void customNamesAndOtherNamespacesAreNotRenamed() {
        assertEquals("script_custom", ArchitectDefinition.parse(definition("script_custom")).id());
        var other = new NamespacedKey("another", "script_nova");
        assertEquals(other, ArchitectIds.canonical(other));
        assertFalse(ScriptArchitectLoader.hasLegacyDefault(directory.resolve("custom.json")));
        assertFalse(ScriptArchitectLoader.hasLegacyDefault(directory.resolve("nova.lua")));
    }

    @Test void legacyIncompatibilitiesReferToTheCanonicalArchitect() {
        String json = """
                {"id":"custom_park","name":"test","description":[],"kind":"park","material":"FEATHER",
                 "backend":"lua","script":"test.lua","incompatible":["otogi:script_nova","another:script_nova"]}
                """;
        var parsed = ArchitectDefinition.parse(json);
        assertTrue(parsed.incompatible().contains(new NamespacedKey("otogi", "nova")));
        assertTrue(parsed.incompatible().contains(new NamespacedKey("another", "script_nova")));
    }
}
