package uk.akkiserver.otogi.api.arena.mode;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.Arena;

public interface Mode {
    @NotNull
    NamespacedKey getId();

    int getDefaultMatchTime();

    default void onStart(Arena arena) {}

    default void onTick(Arena arena) {}

    default void onEnd(Arena arena) {}
}
