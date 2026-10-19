package jp.akkiserver.otogi.api;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import org.bukkit.entity.Player;
import jp.akkiserver.otogi.api.detail.PluginAPI;
import jp.akkiserver.otogi.api.arena.mode.Mode;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class OtogiAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(OtogiAPI.class);
    private static volatile Map<NamespacedKey, Architect> ARCHITECTS = Map.of();
    private static final Map<NamespacedKey, Mode> MODES = new ConcurrentHashMap<>();

    private static final Set<Arena> ARENAS = ConcurrentHashMap.newKeySet();

    public static PluginAPI plugin = null;

    public static void registerArena(@NotNull Arena arena) {
        ARENAS.add(arena);
    }

    public static void unregisterArena(@NotNull Arena arena) {
        ARENAS.remove(arena);
    }

    /** Restores every participant before the plugin is disabled. */
    public static void shutdown() {
        for (Arena arena : List.copyOf(ARENAS)) {
            try {
                arena.end(false);
            } catch (RuntimeException exception) {
                LOGGER.error("Unable to close arena", exception);
            }
        }
        ARENAS.clear();
        ARCHITECTS = Map.of();
        MODES.clear();
    }

    /**
     * Gets the arena the player is playing in.
     */
    @Nullable
    public static ArenaPlayer getArenaPlayer(@NotNull Player player) {
        for (Arena arena : ARENAS) {
            ArenaPlayer arenaPlayer = arena.getPlayer(player);
            if (arenaPlayer != null) {
                return arenaPlayer;
            }
        }
        return null;
    }

    /**
     * Gets all registered architects.
     */
    @NotNull
    public static Collection<Architect> getArchitects() {
        return Collections.unmodifiableCollection(ARCHITECTS.values());
    }

    public static void registerMode(@NotNull Mode mode) {
        if (MODES.putIfAbsent(mode.getId(), mode) != null) {
            throw new IllegalArgumentException("The mode '" + mode.getId() + "' is already registered.");
        }
    }

    @Nullable
    public static Mode getMode(@NotNull NamespacedKey id) {
        return MODES.get(id);
    }

    @NotNull
    public static Collection<Mode> getModes() {
        return MODES.values().stream()
                .sorted(java.util.Comparator.comparing(mode -> mode.getId().toString()))
                .toList();
    }

    /**
     * Registers an architect for use.
     *
     * @param architect the architect to register
     * @throws IllegalArgumentException if the architect is already registered
     */
    public static synchronized void registerArchitect(@NotNull Architect architect) {
        if (ARCHITECTS.containsKey(architect.getId())) {
            throw new IllegalArgumentException("The architect '" + architect.getId() + "' is already registered.");
        }

        var updated = new HashMap<>(ARCHITECTS);
        updated.put(architect.getId(), architect);
        ARCHITECTS = Map.copyOf(updated);

        if (architect instanceof Listener listener) {
            plugin.registerEventListener(listener);
        }
    }

    /** Validate the complete replacement before publishing a single immutable registry snapshot. */
    public static synchronized void replaceArchitects(Collection<? extends Architect> previous,
                                                      Collection<? extends Architect> replacements) {
        ARCHITECTS = replacementSnapshot(previous, replacements);
    }

    public static synchronized void validateArchitectReplacement(Collection<? extends Architect> previous,
                                                                 Collection<? extends Architect> replacements) {
        replacementSnapshot(previous, replacements);
    }

    private static Map<NamespacedKey, Architect> replacementSnapshot(Collection<? extends Architect> previous,
                                                                   Collection<? extends Architect> replacements) {
        Map<NamespacedKey, Architect> updated = new HashMap<>(ARCHITECTS);
        for (Architect architect : previous) updated.remove(architect.getId(), architect);
        for (Architect architect : replacements) {
            Objects.requireNonNull(architect, "architect");
            if (architect instanceof Listener) throw new IllegalArgumentException("Reloadable architects must use hooks, not listeners");
            if (updated.putIfAbsent(architect.getId(), architect) != null)
                throw new IllegalArgumentException("Duplicate architect id: " + architect.getId());
        }
        return Map.copyOf(updated);
    }

    /**
     * Registers multiple architects for use.
     *
     * @param Architects the architects to register
     * @throws IllegalArgumentException if the one of architect is already registered
     */
    public static void registerArchitects(@NotNull Architect... Architects) {
        for (var architect : Architects) {
            registerArchitect(architect);
        }
    }

    /**
     * Registers multiple architects for use.
     *
     * @param architects the architects to register
     * @throws IllegalArgumentException if the one of architect is already registered
     */
    public static void registerArchitects(@NotNull Iterable<@NotNull Architect> architects) {
        for (var architect : architects) {
            registerArchitect(architect);
        }
    }

    /**
     * Gets an architect by its ID.
     *
     * @param id the ID of the architect
     * @return the architect, or {@code null} if the architect is not registered
     */
    @Nullable
    public static Architect getArchitect(@NotNull NamespacedKey id) {
        Architect direct = ARCHITECTS.get(id);
        return direct != null ? direct : ARCHITECTS.get(jp.akkiserver.otogi.api.util.ArchitectIds.canonical(id));
    }

    /**
     * Gets an architect by its ID.
     *
     * @param id the ID of the architect
     * @return the architect, or {@code null} if the architect is not registered
     * @throws IllegalArgumentException if {@code id} is invalid for {@link NamespacedKey}
     */
    @Nullable
    public static Architect getArchitect(@NotNull String id) {
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) {
            throw new IllegalArgumentException("Invalid id: '" + id + "'");
        }
        return getArchitect(key);
    }

    /**
     * Gets an architect by its ID.
     *
     * @param id the ID of the architect
     * @return the architect
     * @throws NoSuchElementException if the architect is not registered
     */
    @NotNull
    public static Architect getArchitectOrThrow(@NotNull NamespacedKey id) {
        final Architect architect = getArchitect(id);

        if (architect == null) {
            throw new NoSuchElementException("No architect is registered with id '" + id + "'.");
        }

        return architect;
    }

    /**
     * Gets an architect by its ID.
     *
     * @param id the ID of the architect
     * @return the architect
     * @throws IllegalArgumentException if {@code id} is invalid for {@link NamespacedKey}
     * @throws NoSuchElementException if the architect is not registered
     */
    @NotNull
    public static Architect getArchitectOrThrow(@NotNull String id) {
        final NamespacedKey key = NamespacedKey.fromString(id);

        if (key == null) {
            throw new IllegalArgumentException("Invalid id: '" + id + "'");
        }

        return getArchitectOrThrow(key);
    }

    private OtogiAPI() {}
}
