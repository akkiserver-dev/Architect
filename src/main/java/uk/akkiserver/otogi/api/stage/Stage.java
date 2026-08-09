package uk.akkiserver.otogi.api.stage;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import uk.akkiserver.otogi.api.util.Position;

import java.io.Serializable;
import java.util.Set;

public interface Stage extends Serializable {
    @NotNull
    Component getTitle();

    @Nullable
    Component getDescription();

    Position getSpectatorSpawn();

    Position getPos1();

    Position getPos2();

    /**
     * Get {@link Set} of {@link StageData} (mode-dependent data)
     */
    @NotNull
    Set<StageData> getData();

    @Nullable
    default StageData getDataFor(@NotNull NamespacedKey id) {
        return getData().stream()
                .filter(data -> data.getModeId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    default <T extends StageData> T getDataFor(@NotNull NamespacedKey id, @NotNull Class<T> type) {
        return getData().stream()
                .filter(data -> data.getModeId().equals(id))
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    default <T extends StageData> T getDataFor(@NotNull Class<T> type) {
        return getData().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);
    }

    default int getWorldTime() {
        return 1000;
    }
}
