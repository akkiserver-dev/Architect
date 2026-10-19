package jp.akkiserver.otogi.api.arena;

import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

public final class MatchTimer extends BukkitRunnable {
    private final Arena arena;
    private int remainingMatchTime;
    private final int totalMatchTime;
    private int ticks;

    public MatchTimer(@NotNull Arena arena, int matchTime) {
        this.arena = arena;
        this.remainingMatchTime = matchTime;
        this.totalMatchTime = matchTime;
    }

    @NotNull
    public Arena getArena() {
        return arena;
    }

    public int getRemainingMatchTime() {
        return remainingMatchTime;
    }

    public int getTotalMatchTime() { return totalMatchTime; }

    @Override
    public void run() {
        if (remainingMatchTime <= 0) {
            cancel();
            arena.end();
            return;
        }

        arena.tick();
        if (++ticks % 20 == 0 && arena.isRunning()) {
            remainingMatchTime--;
            arena.getMode().onTick(arena);
            if (remainingMatchTime <= 0) {
                arena.end();
            }
        }
    }

    public void stop() {
        try {
            cancel();
        } catch (IllegalStateException ignored) {
            // A failed start can end an arena before a task has been assigned.
        }
    }
}
