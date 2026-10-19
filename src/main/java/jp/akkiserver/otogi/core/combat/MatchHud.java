package jp.akkiserver.otogi.core.combat;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Updates existing score entries, avoiding sidebar flicker and preserving pre-match boards via snapshot. */
public final class MatchHud {
    private record Board(Objective objective, List<Component> lines) {}
    private final Map<UUID, Board> boards = new HashMap<>();

    public void update(Arena arena) {
        var ranking = arena.getMode().getRanking(arena);
        for (ArenaPlayer viewer : arena.getPlayers()) {
            var player = viewer.getPlayer();
            if (!player.isOnline()) continue;
            Board board = boards.computeIfAbsent(player.getUniqueId(), ignored -> {
                var scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
                var objective = scoreboard.registerNewObjective("otogi", Criteria.DUMMY, Component.text("御伽", NamedTextColor.GOLD));
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
                objective.numberFormat(NumberFormat.blank());
                player.setScoreboard(scoreboard);
                return new Board(objective, new ArrayList<>());
            });
            List<Component> lines = new ArrayList<>();
            lines.add(arena.getMode().getName().color(NamedTextColor.AQUA));
            lines.add(arena.getStage().getTitle().color(NamedTextColor.GRAY));
            lines.add(Component.empty());
            lines.add(Component.text(viewer.isEliminated() ? "観戦中" : "あなた  " + viewer.getKills() + "撃破 / " + viewer.getDeaths() + "デス", NamedTextColor.WHITE));
            lines.addAll(arena.getMode().getStatusLines(arena, viewer).stream().limit(3).toList());
            lines.add(Component.text("──────────", NamedTextColor.DARK_GRAY));
            int rank = 1;
            for (ArenaPlayer entry : ranking.stream().limit(4).toList()) {
                lines.add(Component.text(rank++ + ". " + entry.getPlayer().getName() + "  " + arena.getMode().getScoreLabel(entry),
                        entry == viewer ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
            }
            lines.add(Component.text("/otogi menu  メニュー", NamedTextColor.DARK_GRAY));
            for (int i = 0; i < lines.size(); i++) {
                var score = board.objective().getScore("line:" + i);
                if (i >= board.lines().size() || !lines.get(i).equals(board.lines().get(i))) score.customName(lines.get(i));
                score.setScore(15 - i);
            }
            for (int i = lines.size(); i < board.lines().size(); i++) board.objective().getScore("line:" + i).resetScore();
            board.lines().clear();
            board.lines().addAll(lines);
        }
    }

    public void remove(UUID player) { boards.remove(player); }
    public void clear() { boards.clear(); }
}
