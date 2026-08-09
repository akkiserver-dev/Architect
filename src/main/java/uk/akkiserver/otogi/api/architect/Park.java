package uk.akkiserver.otogi.api.architect;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface Park extends Architect {
    /**
     * Gets the other ids of incompatible architect for this.
     */
    @NotNull
    Set<NamespacedKey> getIncompatiblearchitects();

    /**
     * Gets whether architect compatible with this.
     */
    @Contract(pure = true)
    default boolean isCompatible(@NotNull Architect architect) {
        return isCompatible(architect.getId());
    }

    /**
     * Gets whether architect incompatible with this.
     */
    @Contract(pure = true)
    default boolean isIncompatible(@NotNull Architect architect) {
        return isIncompatible(architect.getId());
    }

    /**
     * Gets whether architect compatible with this.
     */
    @Contract(pure = true)
    default boolean isCompatible(@NotNull NamespacedKey id) {
        return !isIncompatible(id);
    }

    /**
     * Gets whether architect incompatible with this.
     */
    @Contract(pure = true)
    default boolean isIncompatible(@NotNull NamespacedKey id) {
        return getIncompatiblearchitects().contains(id);
    }
}
