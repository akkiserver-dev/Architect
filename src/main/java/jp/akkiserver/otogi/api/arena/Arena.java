package jp.akkiserver.otogi.api.arena;

import jp.akkiserver.otogi.OtogiPlugin;
import jp.akkiserver.otogi.api.util.ArchitectItems;
import jp.akkiserver.otogi.core.combat.MatchHud;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.api.stage.Stage;
import jp.akkiserver.otogi.core.event.architect.SkillUseEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Arena {
    protected final Set<ArenaPlayer> players = new HashSet<>();
    private final Set<ArenaPlayer> participants = new java.util.LinkedHashSet<>();
    private final java.util.UUID matchId = java.util.UUID.randomUUID();
    protected final Stage stage;
    protected final Mode mode;
    protected final MatchTimer matchTimer;
    private boolean started;
    private boolean ended;
    private int elapsedTicks;
    private final MatchHud hud = new MatchHud();
    private final net.kyori.adventure.bossbar.BossBar clock = net.kyori.adventure.bossbar.BossBar.bossBar(
            Component.empty(), 1, net.kyori.adventure.bossbar.BossBar.Color.BLUE,
            net.kyori.adventure.bossbar.BossBar.Overlay.PROGRESS);

    public Arena(@NotNull Stage stage, @NotNull Mode mode) {
        this.stage = stage;
        this.mode = mode;
        this.matchTimer = new MatchTimer(this, mode.getDefaultMatchTime());
    }

    @NotNull
    public Stage getStage() {
        return stage;
    }

    @NotNull
    public Mode getMode() {
        return mode;
    }

    @NotNull
    public MatchTimer getMatchTimer() {
        return matchTimer;
    }

    public boolean isStarted() {
        return started;
    }

    public boolean isEnded() {
        return ended;
    }

    /**
     * Returns whether the match is in progress.
     */
    public boolean isRunning() {
        return started && !ended;
    }

    @NotNull
    public Set<ArenaPlayer> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public java.util.UUID getMatchId() { return matchId; }
    public Set<ArenaPlayer> getParticipants() { return Collections.unmodifiableSet(participants); }

    public boolean addPlayer(@NotNull Player player) {
        if (started || ended || OtogiAPI.getArenaPlayer(player) != null || players.stream().anyMatch(current -> current.getPlayer().getUniqueId().equals(player.getUniqueId()))) {
            return false;
        }

        ArenaPlayer arenaPlayer = new ArenaPlayer(player, this);
        players.add(arenaPlayer);
        participants.add(arenaPlayer);
        mode.onJoin(this, arenaPlayer);
        return true;
    }

    public boolean removePlayer(@NotNull Player player) {
        ArenaPlayer arenaPlayer = getPlayer(player);
        if (arenaPlayer == null) {
            return false;
        }

        players.remove(arenaPlayer);
        release(arenaPlayer);
        mode.onLeave(this, arenaPlayer);
        return true;
    }

    @Nullable
    public ArenaPlayer getPlayer(@NotNull Player player) {
        return players.stream()
                .filter(current -> current.getPlayer().getUniqueId().equals(player.getUniqueId()))
                .findFirst()
                .orElse(null);
    }

    public void broadcast(@NotNull Component message) {
        players.forEach(player -> player.getPlayer().sendMessage(message));
    }

    public void start() {
        if (started || ended) {
            return;
        }
        started = true;
        try {
            if (OtogiAPI.plugin == null) {
                throw new IllegalStateException("Otogi has not been enabled.");
            }
            OtogiAPI.registerArena(this);
            players.forEach(ArenaPlayer::saveState);
            mode.onStart(this);
            players.forEach(player -> player.getPlayer().showBossBar(clock));
            matchTimer.runTaskTimer(OtogiAPI.plugin.getPlugin(), 1L, 1L);
        } catch (RuntimeException exception) {
            ended = true;
            players.forEach(this::release);
            OtogiAPI.unregisterArena(this);
            hud.clear();
            throw exception;
        }
    }

    public void end() {
        end(true);
    }

    /** Administrative stops restore state without awarding match results. */
    public void end(boolean recordResult) {
        if (ended) {
            return;
        }
        ended = true;
        matchTimer.stop();
        try {
            mode.onStop(this, recordResult);
        } finally {
            if (recordResult && started && OtogiAPI.plugin != null
                    && OtogiAPI.plugin.getPlugin() instanceof OtogiPlugin plugin
                    && plugin.getPlayerStats() != null) {
                try { plugin.getPlayerStats().recordMatch(this); }
                catch (RuntimeException exception) { OtogiAPI.LOGGER.error("Unable to save match result", exception); }
            }
            for (ArenaPlayer player : new ArrayList<>(players)) {
                try {
                    release(player);
                } catch (RuntimeException exception) {
                    OtogiAPI.LOGGER.error("Unable to restore {}", player.getPlayer().getUniqueId(), exception);
                }
            }
            OtogiAPI.unregisterArena(this);
        }
    }

    private void release(@NotNull ArenaPlayer player) {
        hud.remove(player.getPlayer().getUniqueId());
        player.getPlayer().hideBossBar(clock);
        try {
            player.clearArchitects();
        } finally {
            player.setCoolTimeRate(1.0D);
            player.restoreState();
        }
    }

    /**
     * Advances cooldowns every server tick; periodic architects retain their one-second cadence.
     */
    void tick() {
        elapsedTicks++;
        for (ArenaPlayer player : new ArrayList<>(players)) {
            if (player.isEliminated()) {
                if (elapsedTicks % 20 == 0) player.getPlayer().sendActionBar(Component.text("観戦中 / 試合終了までお待ちください", NamedTextColor.GRAY));
                continue;
            }
            player.tickCoolTimes(1);
            if (elapsedTicks % 20 == 0) {
                for (Architect architect : new ArrayList<>(player.getArchitects())) {
                    if (architect instanceof Tickable tickable) {
                        tickable.onTick(player);
                    }
                }
            }
            if (elapsedTicks % 5 == 0) showStatus(player);
        }
        if (elapsedTicks % 20 == 1) {
            hud.update(this);
            int seconds = matchTimer.getRemainingMatchTime();
            clock.name(Component.text(String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60), NamedTextColor.WHITE)
                    .append(Component.text("  /  " + players.size() + "人", NamedTextColor.GRAY)));
            clock.progress(Math.clamp((float) seconds / Math.max(1, matchTimer.getTotalMatchTime()), 0, 1));
            clock.color(seconds <= 30 ? net.kyori.adventure.bossbar.BossBar.Color.RED : net.kyori.adventure.bossbar.BossBar.Color.BLUE);
            Component customName = mode.getBossBarName(this);
            Float customProgress = mode.getBossBarProgress(this);
            if (customName != null) clock.name(customName);
            if (customProgress != null && Float.isFinite(customProgress)) clock.progress(Math.clamp(customProgress, 0, 1));
        }
    }

    private void showStatus(@NotNull ArenaPlayer player) {
        if (player.hasNotice()) return;
        Component bar = Component.text(player.getKills() + "撃破  ", NamedTextColor.GOLD);
        if (player.isInvulnerable()) bar = bar.append(Component.text("加護中  ", NamedTextColor.AQUA));
        Architect held = ArchitectItems.read(player.getPlayer().getInventory().getItemInMainHand(), player);
        if (held instanceof Tool tool) {
            int filled = (int) Math.floor(player.getAttackProgress() * 5);
            bar = bar.append(tool.getName().color(NamedTextColor.WHITE)).append(Component.text(" " + "▰".repeat(filled), NamedTextColor.AQUA))
                    .append(Component.text("▱".repeat(5 - filled) + "  ", NamedTextColor.DARK_GRAY));
        }
        for (Architect architect : player.getArchitects()) {
            if (!(architect instanceof Skill skill)) {
                continue;
            }
            int remaining = player.getRemainingCoolTime(skill);
            bar = bar.append(skill.getName().color(NamedTextColor.AQUA))
                    .append(remaining > 0
                            ? Component.text(" " + (remaining + 19) / 20 + "s  ", NamedTextColor.RED)
                            : Component.text(" 使用可  ", NamedTextColor.GREEN));
        }
        player.getPlayer().sendActionBar(bar);
    }

    /**
     * Uses a skill for the player if it is off cooldown.
     *
     * @return whether the skill was activated
     */
    public boolean useSkill(@NotNull ArenaPlayer player, @NotNull Skill skill) {
        if (!isRunning() || player.isEliminated() || !players.contains(player) || !player.has(skill)) {
            return false;
        }

        Integer silenced = player.getState("effects.silenced.until", Integer.class);
        if (silenced != null && org.bukkit.Bukkit.getCurrentTick() < silenced) {
            player.sendNotice(Component.text("封殺中 — 術を使えない", NamedTextColor.RED));
            return false;
        }

        int remaining = player.getRemainingCoolTime(skill);
        if (remaining > 0) {
            player.sendNotice(Component.text("あと" + String.format(java.util.Locale.ROOT, "%.1f", remaining / 20.0D) + "秒", NamedTextColor.RED));
            return false;
        }

        SkillUseEvent.Pre pre = new SkillUseEvent.Pre(skill, player);
        Bukkit.getPluginManager().callEvent(pre);
        int life = player.getLifeGeneration();
        if (pre.isCancelled() || !skill.onUse(player)) {
            return false;
        }
        if (!isRunning() || !players.contains(player) || life != player.getLifeGeneration()) return false;

        player.setInvulnerable(0);

        player.setCoolTime(skill, skill.getDefaultCoolTime());
        for (Architect architect : new ArrayList<>(player.getArchitects())) {
            architect.onSkillUse(player, skill);
        }
        Bukkit.getPluginManager().callEvent(new SkillUseEvent.Post(skill, player));
        return true;
    }

    /**
     * Performs the attack of one of the player's tools if the attack interval has passed.
     *
     * @return whether the attack was performed
     */
    public boolean attack(@NotNull ArenaPlayer player, @NotNull Tool tool) {
        if (!isRunning() || player.isEliminated() || !players.contains(player) || !player.has(tool) || !player.canAttack()) {
            return false;
        }
        player.setInvulnerable(0);
        int life = player.getLifeGeneration();
        if (!tool.onAttack(player)) {
            return false;
        }
        if (!isRunning() || !players.contains(player) || life != player.getLifeGeneration()) return false;
        player.setAttackCoolTime(tool.getAttackInterval());
        return true;
    }

    /**
     * Handles a lethal hit on a player. The player is not actually killed; the mode decides what happens.
     */
    public void handleDeath(@NotNull ArenaPlayer victim, @Nullable ArenaPlayer killer) {
        if (!isRunning() || !players.contains(victim) || victim.isEliminated()) {
            return;
        }

        if (killer == victim || killer != null && (!players.contains(killer) || killer.isEliminated())) killer = null;

        victim.addDeath();
        if (killer != null) {
            killer.addKill();
            for (Architect architect : new ArrayList<>(killer.getArchitects())) {
                architect.onKill(killer, victim);
            }
        }
        for (ArenaPlayer owner : new ArrayList<>(players)) {
            for (Architect architect : new ArrayList<>(owner.getArchitects())) {
                architect.onPlayerDeath(owner, victim, killer);
            }
        }
        victim.clearLastAttacker();
        mode.onDeath(this, victim, killer);
    }
}
