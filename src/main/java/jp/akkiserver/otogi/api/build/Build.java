package jp.akkiserver.otogi.api.build;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Park;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The architects a player picked for the next match.
 * A build never holds the same architect twice and holds at most one park.
 * Use {@link Builds#add(Build, Architect)} to add with the size and compatibility checks.
 */
public final class Build {
    private final Set<NamespacedKey> ids = new LinkedHashSet<>();

    /**
     * Gets the picked architects in the order they were picked. Architects that are no longer registered are skipped.
     */
    @NotNull
    public List<Architect> getArchitects() {
        List<Architect> result = new ArrayList<>();
        for (NamespacedKey id : ids) {
            Architect architect = OtogiAPI.getArchitect(id);
            if (architect != null) {
                result.add(architect);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Nullable
    public Park getPark() {
        for (Architect architect : getArchitects()) {
            if (architect instanceof Park park) {
                return park;
            }
        }
        return null;
    }

    public boolean has(@NotNull Architect architect) {
        return ids.contains(architect.getId());
    }

    public int size() {
        return ids.size();
    }

    public boolean isEmpty() {
        return ids.isEmpty();
    }

    boolean addUnchecked(@NotNull Architect architect) {
        return ids.add(architect.getId());
    }

    public boolean remove(@NotNull Architect architect) {
        return ids.remove(architect.getId());
    }

    public void clear() {
        ids.clear();
    }

    @NotNull
    Set<NamespacedKey> getIds() {
        return ids;
    }
}
