package jp.akkiserver.otogi.api.util;

import org.bukkit.NamespacedKey;

import java.util.Set;

/** Compatibility names for bundled architects; custom IDs and namespaces are left untouched. */
public final class ArchitectIds {
    private static final Set<String> RENAMED = Set.of("nova", "gale", "mend", "needle", "siphon", "feather",
            "riposte", "narukami", "yukionna", "shuten", "orochi", "inaba", "tamatebako", "kudan",
            "issun", "shura", "hasshaku", "kuchisake", "okuriinu");

    public static String canonicalKey(String key) {
        return key.startsWith("script_") && RENAMED.contains(key.substring(7)) ? key.substring(7) : key;
    }

    public static NamespacedKey canonical(NamespacedKey id) {
        if (!id.getNamespace().equals("otogi")) return id;
        String key = canonicalKey(id.getKey());
        return key.equals(id.getKey()) ? id : new NamespacedKey("otogi", key);
    }

    public static String legacyKey(String key) { return RENAMED.contains(key) ? "script_" + key : null; }

    private ArchitectIds() {}
}
