package jp.akkiserver.otogi.api.arena;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.util.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ArenaPlayer {
    /** Attackers older than this (ticks) are not credited for environmental deaths. */
    private static final int ASSIST_WINDOW_TICKS = 200;

    protected final Player player;
    protected final Set<Architect> architects = new LinkedHashSet<>();
    protected final Arena arena;

    private final Map<String, Integer> coolTimes = new HashMap<>();
    private final Map<String, Object> state = new HashMap<>();
    private final Map<String, Object> matchState = new HashMap<>();
    private int lifeGeneration;
    private int noticeUntil;
    private boolean eliminated;
    private double coolTimeRate = 1.0D;
    private int kills;
    private int deaths;
    private int invulnerableUntil;
    private int attackReadyTick;
    private int attackInterval;
    private ArenaPlayer lastAttacker;
    private int lastAttackTick;

    private PlayerSnapshot snapshot;
    private double cooldownRemainder;

    public ArenaPlayer(Player player, Arena arena) {
        this.player = player;
        this.arena = arena;
    }

    public Player getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    public Set<Architect> getArchitects() {
        return Collections.unmodifiableSet(architects);
    }

    @Nullable
    public <T extends Architect> T getArchitect(@NotNull Class<T> type) {
        return architects.stream().filter(type::isInstance).map(type::cast).findFirst().orElse(null);
    }

    public boolean has(@NotNull Architect architect) {
        return architects.contains(architect);
    }

    public void addArchitect(@NotNull Architect architect) {
        if (architects.add(architect)) {
            architect.onEquip(this);
        }
    }

    public void clearArchitects() {
        for (Architect architect : new ArrayList<>(architects)) {
            architect.onUnequip(this);
        }
        architects.clear();
    }

    /**
     * Re-applies every architect, e.g. after potion effects were cleared.
     */
    public void refreshArchitects() {
        for (Architect architect : architects) {
            architect.onUnequip(this);
        }
        for (Architect architect : architects) {
            architect.onEquip(this);
        }
    }

    /**
     * Replaces the inventory with the items of the equipped tool and skills.
     */
    public void giveItems() {
        player.getInventory().clear();
        int slot = 0;
        for (Architect architect : architects) {
            if (architect instanceof Tool tool) {
                player.getInventory().setItem(slot++, tool.createItem());
            }
        }
        for (Architect architect : architects) {
            if (architect instanceof Skill skill) {
                player.getInventory().setItem(slot++, skill.createItem());
            }
        }
        player.getInventory().setHeldItemSlot(0);
    }

    // ---- attack ----

    public boolean canAttack() {
        return Bukkit.getCurrentTick() >= attackReadyTick;
    }

    public void setAttackCoolTime(int ticks) {
        attackInterval = Math.max(0, ticks);
        attackReadyTick = Bukkit.getCurrentTick() + ticks;
    }

    public int getRemainingAttackTicks() { return Math.max(0, attackReadyTick - Bukkit.getCurrentTick()); }
    public double getAttackProgress() {
        return attackInterval == 0 ? 1 : Math.clamp(1 - (double) getRemainingAttackTicks() / attackInterval, 0, 1);
    }

    // ---- cool time ----

    public int getRemainingCoolTime(@NotNull Architect architect) {
        return coolTimes.getOrDefault(architect.getId().toString(), 0);
    }

    public void setCoolTime(@NotNull Architect architect, int ticks) {
        if (ticks <= 0) {
            coolTimes.remove(architect.getId().toString());
        } else {
            coolTimes.put(architect.getId().toString(), ticks);
        }
    }

    public void reduceCoolTime(@NotNull Architect architect, int ticks) {
        int remaining = getRemainingCoolTime(architect);
        if (remaining > 0) {
            setCoolTime(architect, remaining - ticks);
        }
    }

    /**
     * Reduces the cooldown of every skill.
     */
    public void reduceSkillCoolTimes(int ticks) {
        for (Architect architect : architects) {
            if (architect instanceof Skill) {
                reduceCoolTime(architect, ticks);
            }
        }
    }

    public void clearCoolTimes() {
        coolTimes.clear();
    }

    public double getCoolTimeRate() {
        return coolTimeRate;
    }

    public void setCoolTimeRate(double coolTimeRate) {
        this.coolTimeRate = Double.isFinite(coolTimeRate) ? Math.max(0, coolTimeRate) : 1.0D;
    }

    /**
     * Advances every cooldown by the given amount of ticks, scaled by the cool time rate.
     */
    public void tickCoolTimes(int ticks) {
        Integer jammed = getState("effects.jammed.until", Integer.class);
        boolean slowed = jammed != null && Bukkit.getCurrentTick() < jammed
                && architects.stream().noneMatch(architect -> architect.getId().getKey().equals("curse"));
        double elapsed = ticks * coolTimeRate * (slowed ? 0.5D : 1.0D) + cooldownRemainder;
        int amount = (int) elapsed;
        cooldownRemainder = elapsed - amount;
        for (String key : new ArrayList<>(coolTimes.keySet())) {
            int remaining = coolTimes.get(key) - amount;
            if (remaining <= 0) {
                coolTimes.remove(key);
            } else {
                coolTimes.put(key, remaining);
            }
        }
    }

    // ---- per player architect state ----

    @Nullable
    public <T> T getState(@NotNull String key, @NotNull Class<T> type) {
        Object value = state.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void setState(@NotNull String key, @NotNull Object value) {
        state.put(key, value);
    }

    public void removeState(@NotNull String key) {
        state.remove(key);
    }

    public boolean hasState(@NotNull String key) {
        return state.containsKey(key);
    }

    @Nullable
    public <T> T getMatchState(@NotNull String key, @NotNull Class<T> type) {
        Object value = matchState.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    public void setMatchState(@NotNull String key, @NotNull Object value) { matchState.put(key, value); }
    public int getLifeGeneration() { return lifeGeneration; }
    public boolean isEliminated() { return eliminated; }
    public void setEliminated(boolean eliminated) { this.eliminated = eliminated; }

    /** Keep short combat feedback visible instead of immediately replacing it with the HUD. */
    public void sendNotice(net.kyori.adventure.text.Component message) {
        sendNotice(message, 30);
    }

    public void sendNotice(net.kyori.adventure.text.Component message, int ticks) {
        noticeUntil = Bukkit.getCurrentTick() + Math.clamp(ticks, 1, 200);
        player.sendActionBar(message);
    }

    public boolean hasNotice() { return Bukkit.getCurrentTick() < noticeUntil; }

    // ---- score ----

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addKill() {
        kills++;
    }

    public void addDeath() {
        deaths++;
    }

    // ---- combat ----

    public void setInvulnerable(int ticks) {
        invulnerableUntil = Bukkit.getCurrentTick() + ticks;
    }

    public boolean isInvulnerable() {
        return Bukkit.getCurrentTick() < invulnerableUntil;
    }

    public void setLastAttacker(@Nullable ArenaPlayer attacker) {
        this.lastAttacker = attacker;
        this.lastAttackTick = Bukkit.getCurrentTick();
    }

    /**
     * Gets the last player who damaged this player recently.
     */
    @Nullable
    public ArenaPlayer getRecentAttacker() {
        if (lastAttacker != null && !lastAttacker.isEliminated() && arena.getPlayers().contains(lastAttacker)
                && Bukkit.getCurrentTick() - lastAttackTick <= ASSIST_WINDOW_TICKS) {
            return lastAttacker;
        }
        return null;
    }

    public void clearLastAttacker() {
        lastAttacker = null;
    }

    // ---- state snapshot ----

    /**
     * Stores the inventory, game mode and location, then prepares the player for a match.
     */
    public void saveState() {
        if (snapshot != null) {
            throw new IllegalStateException("Player state is already saved");
        }
        snapshot = new PlayerSnapshot(player);
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.setFlying(false);
        player.setAllowFlight(false);
        clearPotionEffects();
        resetVitals();
    }

    public void restoreState() {
        if (snapshot == null) {
            return;
        }
        snapshot.restore(player, getMaxHealth());
        snapshot = null;
        eliminated = false;
        state.clear();
        matchState.clear();
        lifeGeneration++;
        clearCoolTimes();
        clearLastAttacker();
        cooldownRemainder = 0;
        attackReadyTick = 0;
        invulnerableUntil = 0;
    }

    /** Clears transient effects from the previous life, after unequipping architects. */
    public void resetCombatState() {
        lifeGeneration++;
        noticeUntil = 0;
        state.clear();
        clearCoolTimes();
        clearLastAttacker();
        attackReadyTick = 0;
        cooldownRemainder = 0;
        coolTimeRate = 1;
    }

    // ---- vitals ----

    @Nullable
    public AttributeInstance getAttribute(@NotNull Attribute attribute) {
        return player.getAttribute(attribute);
    }

    public double getMaxHealth() {
        AttributeInstance attribute = getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0D;
    }

    public void setHealth(double health) {
        player.setHealth(Math.clamp(health, 0.0D, getMaxHealth()));
    }

    public void restoreHealth() {
        setHealth(getMaxHealth());
    }

    public void resetVitals() {
        restoreHealth();
        player.setFoodLevel(20);
        player.setSaturation(5.0F);
        player.setExhaustion(0.0F);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0.0F);
        player.setAbsorptionAmount(0);
        player.setRemainingAir(player.getMaximumAir());
        player.setNoDamageTicks(0);
    }

    public void clearPotionEffects() {
        for (PotionEffect effect : List.copyOf(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    public boolean teleport(@NotNull Position position) {
        return player.teleport(position.toLocation());
    }
}
