package jp.akkiserver.otogi.api.stage;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.util.Position;

import java.io.Serializable;
import java.util.Set;
import java.util.List;
import org.bukkit.Location;

public interface Stage extends Serializable {
    default String getId() { return "custom"; }

    default List<Position> getSpawnPositions() { return List.of(getSpectatorSpawn()); }

    default Position getRecoverySpawn() { return getSpawnPositions().getFirst(); }

    default Position getControlPoint() { return getRecoverySpawn(); }

    default double getControlPointRadius() { return 5.0D; }

    /** Inclusive playable bounds. Coordinates must belong to the stage's world. */
    default boolean contains(Location location) {
        Position first = getPos1(), second = getPos2();
        return location.getWorld() != null && location.getWorld().getUID().equals(first.worldId())
                && location.x() >= Math.min(first.x(), second.x()) && location.x() <= Math.max(first.x(), second.x())
                && location.y() >= Math.min(first.y(), second.y()) && location.y() <= Math.max(first.y(), second.y())
                && location.z() >= Math.min(first.z(), second.z()) && location.z() <= Math.max(first.z(), second.z());
    }
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
