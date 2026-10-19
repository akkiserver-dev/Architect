package jp.akkiserver.otogi.api.architect;

import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

public interface Tickable {
    void onTick(@NotNull ArenaPlayer player);
}
