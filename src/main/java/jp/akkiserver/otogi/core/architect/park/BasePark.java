package jp.akkiserver.otogi.core.architect.park;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.architect.Park;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Modifiers;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A park (式) that applies attribute modifiers while equipped.
 */
abstract class BasePark extends BaseArchitect implements Park {
    private final Set<NamespacedKey> incompatible;

    protected BasePark(@NotNull String id, @NotNull String name, @NotNull String[] description, @NotNull String... incompatible) {
        super(id, name, description);
        this.incompatible = Arrays.stream(incompatible).map(key -> new NamespacedKey("otogi", key)).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public @NotNull Set<NamespacedKey> getIncompatiblearchitects() {
        return incompatible;
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) {
        Modifiers.removeAll(player.getPlayer(), this);
    }
}
