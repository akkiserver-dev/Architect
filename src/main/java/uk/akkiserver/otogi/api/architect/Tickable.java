package uk.akkiserver.otogi.api.architect;

import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.ArenaPlayer;

public interface Tickable {
    void onTick(@NotNull ArenaPlayer player);
}
