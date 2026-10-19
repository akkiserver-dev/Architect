package jp.akkiserver.otogi.core.mode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Configured lives, then spectator. Per-match rules are stored separately from transient life state. */
public final class ModeLastStand extends ModeFFA {
    public static final NamespacedKey ID = new NamespacedKey("otogi", "last_stand");
    private static final String LIVES = "mode.last_stand.lives";
    @Override public NamespacedKey getId() { return ID; }
    @Override public Component getName() { return Component.text("残機戦 / Last Stand"); }
    @Override public Component getDescription() { return Component.text("残機" + ModeSettings.get().lastStand().lives() + "。尽きたら観戦へ。最後の生存者が勝利、時間切れは残機と撃破数で判定。"); }
    @Override public int getDefaultMatchTime() { return ModeSettings.get().lastStand().matchSeconds(); }
    @Override protected String objective() { return "残機" + ModeSettings.get().lastStand().lives() + " / 最後の生存者が勝利"; }
    @Override public void onStart(Arena arena) {
        arena.getPlayers().forEach(player -> player.setMatchState(LIVES, ModeSettings.get().lastStand().lives()));
        super.onStart(arena);
    }
    private static int lives(ArenaPlayer player) {
        Integer saved = player.getMatchState(LIVES, Integer.class);
        return saved == null ? ModeSettings.get().lastStand().lives() : saved;
    }

    @Override public void onDeath(Arena arena, ArenaPlayer victim, ArenaPlayer killer) {
        if (victim.getDeaths() < lives(victim)) {
            arena.broadcast(Component.text(victim.getPlayer().getName() + " が倒れた / 残機 " + remaining(victim), NamedTextColor.YELLOW));
            respawn(arena, victim);
            return;
        }
        victim.clearPotionEffects();
        victim.clearArchitects();
        victim.resetCombatState();
        victim.resetVitals();
        victim.setEliminated(true);
        victim.getPlayer().getInventory().clear();
        victim.getPlayer().setGameMode(GameMode.SPECTATOR);
        if (!victim.teleport(arena.getStage().getSpectatorSpawn())) { arena.end(false); return; }
        arena.broadcast(Component.text(victim.getPlayer().getName() + " が脱落。観戦へ移動しました。", NamedTextColor.RED));
        checkEnd(arena);
    }
    @Override public void onLeave(Arena arena, ArenaPlayer player) { checkEnd(arena); }
    private void checkEnd(Arena arena) {
        if (arena.isRunning() && arena.getPlayers().stream().filter(player -> !player.isEliminated()).count() <= 1) arena.end();
    }
    public static int remaining(ArenaPlayer player) { return Math.max(0, lives(player) - player.getDeaths()); }
    @Override public String getScoreLabel(ArenaPlayer player) { return "残機" + remaining(player); }
    @Override public List<ArenaPlayer> getRanking(Arena arena) {
        return arena.getPlayers().stream().sorted(Comparator.comparingInt(ModeLastStand::remaining).reversed()
                .thenComparing(Comparator.comparingInt(ArenaPlayer::getKills).reversed())
                .thenComparing(player -> player.getPlayer().getName())).toList();
    }
    @Override public Set<UUID> getWinners(Arena arena) {
        List<ArenaPlayer> players = getRanking(arena);
        if (players.isEmpty()) return Set.of();
        ArenaPlayer first = players.getFirst();
        if (remaining(first) == 0) return Set.of();
        if (players.size() > 1 && remaining(first) == remaining(players.get(1)) && first.getKills() == players.get(1).getKills()) return Set.of();
        return Set.of(first.getPlayer().getUniqueId());
    }
    @Override public List<Component> getStatusLines(Arena arena, ArenaPlayer viewer) {
        long alive = arena.getPlayers().stream().filter(player -> !player.isEliminated()).count();
        return List.of(Component.text(viewer.isEliminated() ? "脱落 / 観戦中" : "残機: " + remaining(viewer) + " / " + lives(viewer), NamedTextColor.GOLD),
                Component.text("生存者: " + alive + "人", NamedTextColor.GREEN));
    }
    @Override public void onEnd(Arena arena) {
        Set<UUID> winners = getWinners(arena);
        arena.broadcast(Component.text("===== 残機戦 結果 =====", NamedTextColor.GOLD));
        for (ArenaPlayer player : getRanking(arena)) {
            arena.broadcast(Component.text((winners.contains(player.getPlayer().getUniqueId()) ? "勝者 " : "") + player.getPlayer().getName()
                    + " / 残機 " + remaining(player) + " / " + player.getKills() + "撃破", NamedTextColor.WHITE));
        }
        if (winners.isEmpty()) arena.broadcast(Component.text("引き分け", NamedTextColor.YELLOW));
    }
}
