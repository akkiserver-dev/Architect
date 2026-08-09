package uk.akkiserver.otogi.api.architect;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a basic kit unit in the Otogi game plugin.
 */
public interface Architect {
    /**
     * Gets id for this architect.
     * @return The architect id
     */
    @NotNull
    NamespacedKey getId();

    /**
     * Gets name of this architect.
     * @return The architect name
     */
    @NotNull
    Component getName();

    /**
     * Gets description of this architect.
     * @return The architect description in lines
     */
    @NotNull
    List<Component> getDescription();
}
