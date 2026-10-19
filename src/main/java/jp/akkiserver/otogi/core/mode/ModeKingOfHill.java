package jp.akkiserver.otogi.core.mode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One uncontested player in the marked hill earns one point per second. */
public final class ModeKingOfHill extends ModeFFA {
    public static final NamespacedKey ID = new NamespacedKey("otogi", "king_of_hill");
    private static final String TARGET = "mode.king_of_hill.target";
    private static final String SCORE = "mode.king_of_hill.score";
    @Override public NamespacedKey getId() { return ID; }
    @Override public Component getName() { return Component.text("拠点制圧 / King of the Hill"); }
    @Override public Component getDescription() { return Component.text("指定拠点を単独制圧して毎秒1点。複数人で競合中は加点なし。先に" + ModeSettings.get().kingOfHill().targetPoints() + "点で勝利。"); }
    @Override public int getDefaultMatchTime() { return ModeSettings.get().kingOfHill().matchSeconds(); }
    @Override protected String objective() { return "拠点を単独制圧 / " + ModeSettings.get().kingOfHill().targetPoints() + "点先取"; }
    @Override public void onStart(Arena arena) {
        arena.getPlayers().forEach(player -> player.setMatchState(TARGET, ModeSettings.get().kingOfHill().targetPoints()));
        super.onStart(arena);
    }
    private static int target(Arena arena) {
        return arena.getPlayers().stream().map(player -> player.getMatchState(TARGET, Integer.class)).filter(java.util.Objects::nonNull)
                .findFirst().orElse(ModeSettings.get().kingOfHill().targetPoints());
    }
    @Override public List<ArenaPlayer> getRanking(Arena arena) {
        return arena.getPlayers().stream().sorted(Comparator.comparingInt(ModeKingOfHill::score).reversed()
                .thenComparing(player -> player.getPlayer().getName())).toList();
    }
    @Override public String getScoreLabel(ArenaPlayer player) { return score(player) + "点"; }
    public static int score(ArenaPlayer player) {
        Integer score = player.getMatchState(SCORE, Integer.class);
        return score == null ? 0 : score;
    }
    @Override public void onDeath(Arena arena, ArenaPlayer victim, ArenaPlayer killer) {
        arena.broadcast(Component.text(victim.getPlayer().getName() + " が倒れた。拠点へ戻れ！", NamedTextColor.YELLOW));
        respawn(arena, victim);
    }
    private static List<ArenaPlayer> occupants(Arena arena) {
        var center = arena.getStage().getControlPoint().toLocation();
        double radius = arena.getStage().getControlPointRadius();
        return arena.getPlayers().stream().filter(player -> !player.isEliminated()).filter(player -> {
            var location = player.getPlayer().getLocation();
            if (!arena.getStage().contains(location)) return false;
            if (location.getWorld() != center.getWorld() || Math.abs(location.y() - center.y()) > 3) return false;
            double x = location.x() - center.x(), z = location.z() - center.z();
            return x*x + z*z <= radius*radius;
        }).toList();
    }
    @Override public void onTick(Arena arena) {
        var center = arena.getStage().getControlPoint().toLocation();
        double radius = arena.getStage().getControlPointRadius();
        for (int i = 0; i < 24; i++) {
            double angle = 2 * Math.PI * i / 24;
            center.getWorld().spawnParticle(Particle.END_ROD, center.x() + Math.cos(angle)*radius, center.y()+0.2,
                    center.z() + Math.sin(angle)*radius, 1, 0, 0, 0, 0);
        }
        List<ArenaPlayer> inHill = occupants(arena);
        if (inHill.size() != 1) return;
        ArenaPlayer owner = inHill.getFirst();
        int points = score(owner) + 1;
        owner.setMatchState(SCORE, points);
        if (points % 10 == 0) arena.broadcast(Component.text(owner.getPlayer().getName() + " が制圧中: " + points + " / " + target(arena), NamedTextColor.GOLD));
        if (points >= target(arena)) arena.end();
    }
    @Override public List<Component> getStatusLines(Arena arena, ArenaPlayer viewer) {
        var center = arena.getStage().getControlPoint();
        return List.of(Component.text("制圧: " + score(viewer) + " / " + target(arena) + "点", NamedTextColor.GOLD),
                Component.text("拠点: " + (int)center.x() + ", " + (int)center.y() + ", " + (int)center.z(), NamedTextColor.YELLOW),
                Component.text(hillStatus(arena), NamedTextColor.AQUA));
    }
    private static String hillStatus(Arena arena) {
        var players = occupants(arena);
        return players.isEmpty() ? "拠点は空いている" : players.size() > 1 ? "拠点競合中 / 加点停止" : players.getFirst().getPlayer().getName() + " が制圧中";
    }
    @Override public Component getBossBarName(Arena arena) {
        int seconds = arena.getMatchTimer().getRemainingMatchTime();
        return Component.text(hillStatus(arena) + " / 最高 " + topScore(arena) + "/" + target(arena) + "点 / " + seconds/60 + ":" + String.format(java.util.Locale.ROOT, "%02d", seconds%60));
    }
    @Override public Float getBossBarProgress(Arena arena) { return Math.clamp((float)topScore(arena)/target(arena), 0, 1); }
    private static int topScore(Arena arena) { return arena.getPlayers().stream().mapToInt(ModeKingOfHill::score).max().orElse(0); }
    @Override public Set<UUID> getWinners(Arena arena) {
        int top = topScore(arena);
        var winners = arena.getPlayers().stream().filter(player -> score(player) == top).toList();
        return winners.size() == 1 && top > 0 ? Set.of(winners.getFirst().getPlayer().getUniqueId()) : Set.of();
    }
    @Override public void onEnd(Arena arena) {
        var winners = getWinners(arena);
        arena.broadcast(Component.text("===== 拠点制圧 結果 =====", NamedTextColor.GOLD));
        arena.getPlayers().stream().sorted(Comparator.comparingInt(ModeKingOfHill::score).reversed()).forEach(player ->
                arena.broadcast(Component.text((winners.contains(player.getPlayer().getUniqueId()) ? "勝者 " : "") + player.getPlayer().getName()
                        + " / " + score(player) + "点 / " + player.getKills() + "撃破", NamedTextColor.WHITE)));
        if (winners.isEmpty()) arena.broadcast(Component.text("引き分け", NamedTextColor.YELLOW));
    }
}
