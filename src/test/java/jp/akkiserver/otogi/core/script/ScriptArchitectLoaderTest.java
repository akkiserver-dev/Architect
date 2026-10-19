package jp.akkiserver.otogi.core.script;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ScriptArchitectLoaderTest {
    @TempDir Path directory;
    @AfterEach void clear() { OtogiAPI.shutdown(); }

    private static String definition(String id, int cooldown) {
        return """
                {"id":"%s", "name":"試験", "description":["test"], "kind":"skill", "material":"FEATHER",
                 "backend":"java", "handler":"dash", "cooldown":%d, "parameters":{"speed":1.2}}
                """.formatted(id, cooldown);
    }

    @Test void malformedBatchKeepsAllPreviousInstancesAndNewValidBatchReplacesThem() throws Exception {
        Files.writeString(directory.resolve("first.json"), definition("first", 100));
        Files.writeString(directory.resolve("second.json"), definition("second", 100));
        ScriptArchitectLoader loader = new ScriptArchitectLoader(directory);
        assertTrue(loader.reload().success());
        var first = OtogiAPI.getArchitect("otogi:first");
        var second = OtogiAPI.getArchitect("otogi:second");
        Files.writeString(directory.resolve("first.json"), definition("first", 50));
        Files.writeString(directory.resolve("second.json"), "{broken");
        assertFalse(loader.reload().success());
        assertSame(first, OtogiAPI.getArchitect("otogi:first"));
        assertSame(second, OtogiAPI.getArchitect("otogi:second"));
        Files.delete(directory.resolve("second.json"));
        assertTrue(loader.reload().success());
        assertNotSame(first, OtogiAPI.getArchitect("otogi:first"));
        assertEquals(100, ((ScriptArchitect) first).getDefinition().cooldown(), "Old snapshots are immutable");
        assertNull(OtogiAPI.getArchitect("otogi:second"));
    }

    @Test void preparationDoesNotPublishUntilCommitAndValidatesBuiltinCollisions() throws Exception {
        Files.writeString(directory.resolve("first.json"), definition("first", 100));
        ScriptArchitectLoader loader = new ScriptArchitectLoader(directory);
        assertTrue(loader.reload().success());
        var current = OtogiAPI.getArchitect("otogi:first");
        Files.writeString(directory.resolve("first.json"), definition("first", 50));
        var prepared = loader.prepareReload();
        assertSame(current, OtogiAPI.getArchitect("otogi:first"));
        loader.commitReload(prepared);
        assertNotSame(current, OtogiAPI.getArchitect("otogi:first"));
        assertEquals(50, ((ScriptArchitect) OtogiAPI.getArchitect("otogi:first")).getDefinition().cooldown());

        OtogiAPI.registerArchitect(new BaseArchitect("builtin", "builtin") {});
        Files.writeString(directory.resolve("second.json"), definition("builtin", 100));
        assertThrows(IllegalArgumentException.class, loader::prepareReload);
        assertSame(prepared.getFirst(), OtogiAPI.getArchitect("otogi:first"));
    }

    @Test void duplicateBuiltinAndDuplicateFilesNeverReplaceExistingDefinitions() throws Exception {
        var builtin = new BaseArchitect("builtin", "builtin") {};
        OtogiAPI.registerArchitect(builtin);
        Files.writeString(directory.resolve("first.json"), definition("builtin", 100));
        ScriptArchitectLoader loader = new ScriptArchitectLoader(directory);
        assertFalse(loader.reload().success());
        assertSame(builtin, OtogiAPI.getArchitect("otogi:builtin"));
        Files.writeString(directory.resolve("first.json"), definition("duplicate", 100));
        Files.writeString(directory.resolve("second.json"), definition("duplicate", 100));
        assertFalse(loader.reload().success());
        assertNull(OtogiAPI.getArchitect("otogi:duplicate"));
    }

    @Test void invalidLuaAndEscapePathsKeepRegistry() throws Exception {
        Files.writeString(directory.resolve("first.json"), definition("first", 100));
        ScriptArchitectLoader loader = new ScriptArchitectLoader(directory);
        assertTrue(loader.reload().success());
        var first = OtogiAPI.getArchitect("otogi:first");
        String luaDefinition = definition("first", 100).replace("\"backend\":\"java\", \"handler\":\"dash\"", "\"backend\":\"lua\", \"script\":\"bad.lua\"");
        Files.writeString(directory.resolve("first.json"), luaDefinition);
        Files.writeString(directory.resolve("bad.lua"), "while true do end");
        assertFalse(loader.reload().success());
        assertSame(first, OtogiAPI.getArchitect("otogi:first"));
        Files.writeString(directory.resolve("first.json"), luaDefinition.replace("bad.lua", "../outside.lua"));
        assertFalse(loader.reload().success());
        assertSame(first, OtogiAPI.getArchitect("otogi:first"));
    }

    @Test void bundledDefinitionsValidateAndSpanAllFourKinds() throws Exception {
        var definitions = ScriptArchitectLoader.loadDefinitions(Path.of("src/main/resources/architects"));
        assertEquals(19, definitions.size());
        assertEquals(4, definitions.stream().map(a -> ((ScriptArchitect) a).getDefinition().kind()).distinct().count());
        for (var architect : definitions) {
            var data = ((ScriptArchitect) architect).getDefinition();
            assertFalse(data.id().startsWith("script_"), "Bundled IDs should describe content, not its implementation");
            String folder = switch (data.kind()) {
                case SKILL -> "skills";
                case TOOL -> "tools";
                case PARK -> "parks";
                case PASSIVE -> "passives";
            };
            assertTrue(Files.isRegularFile(Path.of("docs/game/draft", folder, data.id() + ".md")),
                    () -> "Missing draft specification for " + data.id());
        }
    }

    @Test void rejectsWrongTypesUnknownKeysAndUnsafeBounds() {
        assertThrows(RuntimeException.class, () -> ArchitectDefinition.parse(definition("invalid", -1)));
        assertThrows(RuntimeException.class, () -> ArchitectDefinition.parse(definition("invalid", 100).replace("1.2", "\"fast\"")));
        assertThrows(RuntimeException.class, () -> ArchitectDefinition.parse(definition("invalid", 100).replace("\"speed\"", "\"illegal.key\"")));
        assertThrows(RuntimeException.class, () -> ArchitectDefinition.parse(definition("invalid", 100).replace("\"parameters\"", "\"paramters\"")));
    }
}
