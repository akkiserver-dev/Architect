package uk.akkiserver.otogi.api;

import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.akkiserver.otogi.api.architect.Architect;
import uk.akkiserver.otogi.api.detail.PluginAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class OtogiAPI {
    public static final Logger LOGGER = LoggerFactory.getLogger(OtogiAPI.class);
    private static final Map<NamespacedKey, Architect> ARCHITECTS = new ConcurrentHashMap<>();

    public static PluginAPI plugin = null;

    /**
     * Registers an architect for use.
     *
     * @param architect the architect to register
     * @throws IllegalArgumentException if the architect is already registered
     */
    public static void registerArchitect(@NotNull Architect architect) {
        if (ARCHITECTS.putIfAbsent(architect.getId(), architect) != null) {
            throw new IllegalArgumentException("The architect '" + architect.getId() + "' is already registered.");
        }

        if (architect instanceof Listener listener) {
            plugin.registerEventListener(listener);
        }
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
        return ARCHITECTS.get(id);
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
        return ARCHITECTS.get(key);
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
