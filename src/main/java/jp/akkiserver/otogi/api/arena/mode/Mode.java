package jp.akkiserver.otogi.api.arena.mode;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.stage.StageData;
import jp.akkiserver.otogi.api.util.Position;

import java.util.Collection;

public interface Mode {
    @NotNull
    NamespacedKey getId();

    int getDefaultMatchTime();

    default net.kyori.adventure.text.Component getName() {
        return net.kyori.adventure.text.Component.text(getId().getKey());
    }

    default net.kyori.adventure.text.Component getDescription() {
        return net.kyori.adventure.text.Component.empty();
    }

    default java.util.List<net.kyori.adventure.text.Component> getStatusLines(Arena arena, ArenaPlayer viewer) {
        return java.util.List.of();
    }

    default net.kyori.adventure.text.Component getBossBarName(Arena arena) { return null; }
    default Float getBossBarProgress(Arena arena) { return null; }

    default java.util.List<ArenaPlayer> getRanking(Arena arena) {
        return arena.getPlayers().stream().sorted(java.util.Comparator.comparingInt(ArenaPlayer::getKills).reversed()
                .thenComparingInt(ArenaPlayer::getDeaths).thenComparing(player -> player.getPlayer().getName())).toList();
    }

    default String getScoreLabel(ArenaPlayer player) { return player.getKills() + "撃破"; }

    /** Empty means a draw or no result. Modes with custom scoring should override this. */
    default java.util.Set<java.util.UUID> getWinners(Arena arena) {
        var ranking = arena.getPlayers().stream()
                .sorted(java.util.Comparator.comparingInt(ArenaPlayer::getKills).reversed()
                        .thenComparingInt(ArenaPlayer::getDeaths)).toList();
        if (ranking.isEmpty()) return java.util.Set.of();
        var top = ranking.getFirst();
        if (ranking.size() > 1 && ranking.get(1).getKills() == top.getKills()
                && ranking.get(1).getDeaths() == top.getDeaths()) return java.util.Set.of();
        return java.util.Set.of(top.getPlayer().getUniqueId());
    }

    default void onStart(Arena arena) {}

    default void onJoin(Arena arena, ArenaPlayer player) {}

    default void onLeave(Arena arena, ArenaPlayer player) {}

    /**
     * Called when a player took a lethal hit. The player is still alive and must be handled by the mode.
     */
    default void onDeath(Arena arena, ArenaPlayer victim, @Nullable ArenaPlayer killer) {}

    default StageData createCommandStageData(@NotNull Collection<Position> spawns) {
        return null;
    }

    default void onTick(Arena arena) {}

    default void onEnd(Arena arena) {}

    default void onStop(Arena arena, boolean completed) {
        if (completed) onEnd(arena);
        else arena.broadcast(net.kyori.adventure.text.Component.text("試合を中止しました。戦績には記録されません。",
                net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }
}
