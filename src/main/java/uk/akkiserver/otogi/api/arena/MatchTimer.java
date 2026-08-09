package uk.akkiserver.otogi.api.util;

import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import uk.akkiserver.otogi.api.arena.Arena;

public final class MatchTimer extends BukkitRunnable {
    private final Arena arena;
    private int remainingMatchTime;

    public MatchTimer(@NotNull Arena arena, int matchTime) {
        this.arena = arena;
        this.remainingMatchTime = matchTime;
    }

    @NotNull
    public Arena getArena() {
        return arena;
    }

    public int getRemainingMatchTime() {
        return remainingMatchTime;
    }

    @Override
    public void run() {
        if (remainingMatchTime <= 0) {
            cancel();
            arena.end();
            return;
        }

        remainingMatchTime--;
        arena.getMode().onTick(arena);
    }
}