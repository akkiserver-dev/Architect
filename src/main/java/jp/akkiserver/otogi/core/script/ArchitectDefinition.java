package jp.akkiserver.otogi.core.script;

import com.google.gson.*;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.*;

/** Strict, bounded definition schema. Cooldown and intervals are in server ticks. */
public record ArchitectDefinition(String id, String name, List<String> description, Kind kind, Material material,
                                  int cooldown, int attackInterval, int tickInterval, String backend, String handler,
                                  String script, Map<String, Double> parameters, Set<NamespacedKey> incompatible) {
    public enum Kind { SKILL, PASSIVE, PARK, TOOL }
    public static final Set<String> JAVA_HANDLERS = Set.of("pulse", "lance", "dash", "mend", "thunder", "frost_cone", "oni_frenzy", "dread_sphere");

    public static ArchitectDefinition parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Set<String> fields = Set.of("id", "name", "description", "kind", "material", "cooldown", "attackInterval",
                "tickInterval", "backend", "handler", "script", "parameters", "incompatible");
        for (String key : root.keySet()) if (!fields.contains(key)) throw new IllegalArgumentException("Unknown field: " + key);
        String id = jp.akkiserver.otogi.api.util.ArchitectIds.canonicalKey(string(root, "id", null));
        if (!id.matches("[a-z0-9_/-]{1,64}")) throw new IllegalArgumentException("id must be a lowercase otogi key");
        String name = string(root, "name", null);
        List<String> description = new ArrayList<>();
        if (!root.has("description") || !root.get("description").isJsonArray()) throw new IllegalArgumentException("description must be an array");
        for (JsonElement line : root.getAsJsonArray("description")) description.add(text(line, "description"));
        if (description.size() > 12) throw new IllegalArgumentException("At most 12 description lines");
        Kind kind = Kind.valueOf(string(root, "kind", null).toUpperCase(Locale.ROOT));
        Material material = Material.matchMaterial(string(root, "material", null));
        if (material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR)
            throw new IllegalArgumentException("material must be an item");
        String backend = string(root, "backend", "lua");
        String handler = string(root, "handler", "");
        String script = string(root, "script", "");
        if (!backend.equals("java") && !backend.equals("lua")) throw new IllegalArgumentException("backend must be java or lua");
        if (backend.equals("java") && (!JAVA_HANDLERS.contains(handler) || (kind != Kind.SKILL && kind != Kind.TOOL)))
            throw new IllegalArgumentException("Java handler requires skill/tool and a supported handler");
        if (backend.equals("java") && MythicSkillHandlers.supports(handler) && kind != Kind.SKILL)
            throw new IllegalArgumentException("Mythic handlers require kind=skill");
        if (backend.equals("lua") && !script.matches("[a-zA-Z0-9_/-]+\\.lua")) throw new IllegalArgumentException("script must be a relative .lua path");
        Map<String, Double> parameters = new LinkedHashMap<>();
        if (root.has("parameters")) for (var entry : root.getAsJsonObject("parameters").entrySet()) {
            if (!entry.getKey().matches("[a-zA-Z][a-zA-Z0-9_]{0,47}")) throw new IllegalArgumentException("Invalid parameter key");
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Parameters must be numbers");
            double value = entry.getValue().getAsDouble();
            if (!Double.isFinite(value) || Math.abs(value) > 72_000) throw new IllegalArgumentException("Parameter out of range: " + entry.getKey());
            parameters.put(entry.getKey(), value);
        }
        if (parameters.size() > 64) throw new IllegalArgumentException("At most 64 parameters");
        if (backend.equals("java")) MythicSkillHandlers.validate(handler, parameters);
        Set<NamespacedKey> incompatible = new HashSet<>();
        if (root.has("incompatible")) for (JsonElement element : root.getAsJsonArray("incompatible")) {
            String key = text(element, "incompatible");
            NamespacedKey parsed = NamespacedKey.fromString(key.contains(":") ? key : "otogi:" + key);
            if (parsed == null) throw new IllegalArgumentException("Invalid incompatible id: " + key);
            incompatible.add(jp.akkiserver.otogi.api.util.ArchitectIds.canonical(parsed));
        }
        return new ArchitectDefinition(id, name, List.copyOf(description), kind, material,
                integer(root, "cooldown", 100, 1, 72_000), integer(root, "attackInterval", 16, 2, 1200),
                integer(root, "tickInterval", 20, 20, 1200), backend, handler, script, Map.copyOf(parameters), Set.copyOf(incompatible));
    }

    public double parameter(String key, double fallback) { return parameters.getOrDefault(key, fallback); }
    private static int integer(JsonObject root, String key, int fallback, int min, int max) {
        if (!root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key + " must be an integer");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < min || number > max) throw new IllegalArgumentException(key + " out of range");
        return (int) number;
    }
    private static String string(JsonObject root, String key, String fallback) {
        if (!root.has(key)) {
            if (fallback == null) throw new IllegalArgumentException("Missing " + key);
            return fallback;
        }
        return text(root.get(key), key);
    }
    private static String text(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) throw new IllegalArgumentException(key + " must be a string");
        String value = element.getAsString();
        if (value.length() > 256) throw new IllegalArgumentException(key + " exceeds 256 characters");
        return value;
    }
}
