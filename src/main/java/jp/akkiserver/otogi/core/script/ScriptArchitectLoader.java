package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Validate the entire directory before replacing anything. Call on the server main thread. */
public final class ScriptArchitectLoader {
    private final Path directory;
    private List<Architect> current = List.of();
    public ScriptArchitectLoader(Path directory) { this.directory = directory.toAbsolutePath().normalize(); }

    public record ReloadResult(boolean success, int count, String message) {}

    /** Keep operator-edited legacy files as the sole definition instead of adding a duplicate default. */
    public static boolean hasLegacyDefault(Path target) {
        String filename = target.getFileName().toString();
        if (!filename.endsWith(".json")) return false;
        String legacy = jp.akkiserver.otogi.api.util.ArchitectIds.legacyKey(filename.substring(0, filename.length() - 5));
        return legacy != null && Files.isRegularFile(target.resolveSibling(legacy + ".json"));
    }

    /** Compile and validate a candidate snapshot without changing the live registry. */
    public List<Architect> prepareReload() throws IOException {
        List<Architect> replacements = loadDefinitions(directory);
        OtogiAPI.validateArchitectReplacement(current, replacements);
        return replacements;
    }

    /** Publish a prepared snapshot on the main thread after all configuration validation succeeds. */
    public void commitReload(List<Architect> replacements) {
        List<Architect> snapshot = List.copyOf(replacements);
        OtogiAPI.replaceArchitects(current, snapshot);
        current = snapshot;
    }

    public ReloadResult reload() {
        try {
            commitReload(prepareReload());
            return new ReloadResult(true, current.size(), current.size() + "件の定義を反映。次の試合から適用します。");
        } catch (IOException | RuntimeException exception) {
            return new ReloadResult(false, current.size(), "能力定義の反映中止（現行を維持）: " + exception.getMessage());
        }
    }

    /** Read-only validation entry point, also used by tests and deployment checks. */
    public static List<Architect> loadDefinitions(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) throw new IOException("Architect directory missing: " + directory);
        Path root = directory.toRealPath();
        List<Path> files;
        try (var stream = Files.list(root)) {
            files = stream.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        if (files.size() > 128) throw new IOException("At most 128 architect definitions");
        List<Architect> result = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (Path file : files) {
            try {
                ArchitectDefinition definition = ArchitectDefinition.parse(read(root, file));
                // Paper 26 resolves item eligibility through server registries; pure file validation can run offline.
                if (org.bukkit.Bukkit.getServer() != null && !definition.material().isItem())
                    throw new IllegalArgumentException("material must be an item");
                if (!ids.add(definition.id())) throw new IllegalArgumentException("Duplicate id: " + definition.id());
                LuaSandbox script = definition.backend().equals("lua")
                        ? new LuaSandbox(read(root, root.resolve(definition.script())), definition.script()) : null;
                result.add(ScriptArchitect.create(definition, script));
            } catch (RuntimeException | IOException exception) {
                throw new IOException(file.getFileName() + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(result);
    }

    private static String read(Path root, Path path) throws IOException {
        Path actual = path.toRealPath();
        if (!actual.startsWith(root) || !Files.isRegularFile(actual)) throw new IOException("File must stay inside architects directory");
        if (Files.size(actual) > 32_768) throw new IOException("File exceeds 32 KiB: " + path.getFileName());
        return Files.readString(actual, StandardCharsets.UTF_8);
    }
}
