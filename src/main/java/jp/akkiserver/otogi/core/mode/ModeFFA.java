package jp.akkiserver.otogi.core.mode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.api.stage.StageData;
import jp.akkiserver.otogi.api.util.Position;
import jp.akkiserver.otogi.core.architect.Architects;
import jp.akkiserver.otogi.api.OtogiAPI;
import org.bukkit.Sound;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import jp.akkiserver.otogi.core.combat.CombatEffects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class ModeFFA implements Mode {
    public static final NamespacedKey ID = new NamespacedKey("otogi", "ffa");
    public static final int DEFAULT_MATCH_TIME = 600;
    /** The match ends immediately when a player reaches this many kills. */
    public static final int KILL_LIMIT = 10;
    public static final int SPAWN_PROTECTION_TICKS = 60;

    @Override
    public @NotNull NamespacedKey getId() {
        return ID;
    }

    @Override public Component getName() { return Component.text("乱戦 / FFA"); }
    @Override public Component getDescription() { return Component.text("倒されても即復帰。先に目標撃破数へ到達した者が勝利。"); }

    @Override
    public int getDefaultMatchTime() {
        return setting("match-seconds", DEFAULT_MATCH_TIME, 30, 7200);
    }

    private static int setting(String key, int fallback, int min, int max) {
        return Math.clamp(OtogiAPI.plugin == null ? fallback : OtogiAPI.plugin.getConfig().getInt("ffa." + key, fallback), min, max);
    }

    private static int killLimit() { return setting("kill-limit", KILL_LIMIT, 1, 1000); }
    protected static int protection() { return setting("spawn-protection-ticks", SPAWN_PROTECTION_TICKS, 0, 600); }

    @Override
    public FFAStageData createCommandStageData(@NotNull Collection<Position> spawns) {
        return new FFAStageData(spawns);
    }

    @Override
    public void onStart(@NotNull Arena arena) {
        FFAStageData data = arena.getStage().getDataFor(FFAStageData.class);
        if (data == null || data.spawns().isEmpty()) {
            throw new IllegalStateException("FFA requires at least one spawn position.");
        }

        List<Position> spawns = data.spawns();
        spawns.forEach(spawn -> {
            if (spawn.getWorld() == null) {
                throw new IllegalStateException("FFA spawn position has no loaded world.");
            }
        });
        int offset = ThreadLocalRandom.current().nextInt(spawns.size());
        int index = 0;
        for (ArenaPlayer arenaPlayer : arena.getPlayers()) {
            Position spawn = spawns.get((offset + index++) % spawns.size());
            if (!arenaPlayer.teleport(spawn)) throw new IllegalStateException("Spawn teleport was cancelled");
            Architects.assign(arenaPlayer);
            arenaPlayer.giveItems();
            arenaPlayer.resetVitals();
            arenaPlayer.setInvulnerable(protection());
            announceLoadout(arenaPlayer);
            arenaPlayer.getPlayer().showTitle(Title.title(Component.text("開戦", NamedTextColor.GOLD),
                    Component.text("左クリックで具を振る / 右クリックで術を使う", NamedTextColor.WHITE),
                    Title.Times.times(Duration.ofMillis(150), Duration.ofSeconds(2), Duration.ofMillis(500))));
            CombatEffects.personalSound(arenaPlayer.getPlayer(), Sound.BLOCK_END_PORTAL_SPAWN, 0.35F, 1.4F);
        }
        int seconds = arena.getMatchTimer().getRemainingMatchTime();
        arena.broadcast(Component.text(objective() + " / 制限時間 "
                + String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60), NamedTextColor.GREEN));
    }

    protected String objective() { return killLimit() + "キル先取"; }

    @Override public List<Component> getStatusLines(Arena arena, ArenaPlayer player) {
        return List.of(Component.text("目標: " + killLimit() + "撃破", NamedTextColor.GOLD));
    }

    private void announceLoadout(ArenaPlayer player) {
        Component line = Component.text("今回のビルド: ", NamedTextColor.YELLOW);
        boolean first = true;
        for (Architect architect : player.getArchitects()) {
            line = line.append(first ? Component.empty() : Component.text(" / ", NamedTextColor.GRAY))
                    .append(architect.getName().color(NamedTextColor.WHITE));
            first = false;
        }
        player.getPlayer().sendMessage(line);
    }

    @Override
    public void onDeath(@NotNull Arena arena, @NotNull ArenaPlayer victim, @Nullable ArenaPlayer killer) {
        Component message = killer == null
                ? Component.text(victim.getPlayer().getName() + " が倒れた", NamedTextColor.GRAY)
                : Component.text(killer.getPlayer().getName() + " が " + victim.getPlayer().getName() + " を撃破 (" + killer.getKills() + "キル)", NamedTextColor.RED);
        arena.broadcast(message);

        victim.getPlayer().showTitle(Title.title(Component.text("撃破された", NamedTextColor.RED),
                Component.text(killer == null ? "すぐに戦線へ戻れる" : killer.getPlayer().getName() + " に倒された", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(800), Duration.ofMillis(300))));
        CombatEffects.personalSound(victim.getPlayer(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.5F, 1.3F);
        if (killer != null) CombatEffects.personalSound(killer.getPlayer(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, 1.2F);

        if (killer != null && killer.getKills() >= killLimit()) {
            arena.end();
        } else {
            respawn(arena, victim);
        }
    }

    protected void respawn(Arena arena, ArenaPlayer victim) {
        victim.clearPotionEffects();
        List<Architect> loadout = List.copyOf(victim.getArchitects());
        victim.clearArchitects();
        victim.resetCombatState();
        loadout.forEach(victim::addArchitect);
        victim.resetVitals();
        victim.getPlayer().setVelocity(new Vector());

        FFAStageData data = arena.getStage().getDataFor(FFAStageData.class);
        if (data != null) {
            if (!victim.teleport(farthestSpawn(data.spawns(), arena, victim))) {
                // A cancelled respawn must not leave the player at a lethal/invalid location.
                arena.end(false);
                return;
            }
        }
        victim.setInvulnerable(protection());
    }

    /**
     * Picks the spawn whose nearest opponent is the farthest away.
     */
    private Position farthestSpawn(List<Position> spawns, Arena arena, ArenaPlayer victim) {
        return spawns.stream()
                .max(Comparator.comparingDouble(spawn -> nearestOpponentDistance(spawn, arena, victim)))
                .orElseThrow();
    }

    private double nearestOpponentDistance(Position spawn, Arena arena, ArenaPlayer victim) {
        Location location = spawn.toLocation();
        double nearest = Double.MAX_VALUE;
        for (ArenaPlayer other : arena.getPlayers()) {
            Location current = other.getPlayer().getLocation();
            if (other != victim && !other.isEliminated() && current.getWorld() == location.getWorld()) {
                nearest = Math.min(nearest, current.distanceSquared(location));
            }
        }
        return nearest;
    }

    @Override
    public void onLeave(@NotNull Arena arena, @NotNull ArenaPlayer player) {
        if (arena.isRunning() && arena.getPlayers().size() < 2) {
            arena.end();
        }
    }

    @Override
    public void onEnd(@NotNull Arena arena) {
        List<ArenaPlayer> ranking = new ArrayList<>(arena.getPlayers());
        ranking.sort(Comparator.comparingInt(ArenaPlayer::getKills).reversed().thenComparingInt(ArenaPlayer::getDeaths));
        if (ranking.isEmpty()) {
            return;
        }

        ArenaPlayer top = ranking.getFirst();
        boolean draw = ranking.size() > 1 && ranking.get(1).getKills() == top.getKills() && ranking.get(1).getDeaths() == top.getDeaths();

        Component result = Component.text("===== 結果 =====", NamedTextColor.GOLD)
                .appendNewline()
                .append(Component.text(draw ? "引き分け" : top.getPlayer().getName() + " の勝利！", NamedTextColor.YELLOW));
        int rank = 1;
        for (ArenaPlayer player : ranking) {
            result = result.appendNewline().append(Component.text(
                    rank++ + ". " + player.getPlayer().getName() + "  " + player.getKills() + "K / " + player.getDeaths() + "D", NamedTextColor.WHITE));
        }
        arena.broadcast(result);
        for (ArenaPlayer player : ranking) {
            player.getPlayer().showTitle(Title.title(Component.text(draw ? "引き分け" : top.getPlayer().getName() + " の勝利", NamedTextColor.GOLD),
                    Component.text(player.getKills() + "撃破 / " + player.getDeaths() + "デス", NamedTextColor.WHITE)));
            CombatEffects.personalSound(player.getPlayer(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5F, 1.1F);
        }
    }

    public record FFAStageData(List<Position> spawns) implements StageData {
        public FFAStageData(@NotNull Collection<Position> spawns) {
            this(List.copyOf(spawns));
        }

        public FFAStageData {
            Objects.requireNonNull(spawns, "spawns");
            if (spawns.isEmpty()) {
                throw new IllegalArgumentException("spawns must not be empty");
            }
        }

        @Override
        public @NotNull NamespacedKey getModeId() {
            return ID;
        }
    }
}
