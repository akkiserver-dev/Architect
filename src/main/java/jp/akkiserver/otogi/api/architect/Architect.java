package jp.akkiserver.otogi.api.architect;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.util.List;

/**
 * Represents a basic kit unit in the Otogi game plugin.
 * <p>
 * Architects are shared singletons. Per-player state must be stored in {@link ArenaPlayer}.
 */
public interface Architect {
    /**
     * Gets id for this architect.
     * @return The architect id
     */
    @NotNull
    NamespacedKey getId();

    /**
     * Gets name of this architect.
     * @return The architect name
     */
    @NotNull
    Component getName();

    /**
     * Gets description of this architect.
     * @return The architect description in lines
     */
    @NotNull
    List<Component> getDescription();

    /**
     * Called when this architect is given to the player.
     */
    default void onEquip(@NotNull ArenaPlayer player) {}

    /**
     * Called when this architect is taken from the player.
     */
    default void onUnequip(@NotNull ArenaPlayer player) {}

    /**
     * Called when the owner deals damage to another arena player. The event may be modified.
     */
    default void onDamageDealt(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @NotNull EntityDamageByEntityEvent event) {}

    /** Called after architect defenses accepted a hit. Use for hit-triggered procs. */
    default void onDamageConfirmed(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @NotNull EntityDamageByEntityEvent event) {}

    /**
     * Called when the owner takes damage that is not self inflicted.
     * The event may be modified or cancelled.
     */
    default void onDamageTaken(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {}

    /** Hit-triggered reactions, after all architect defenses have accepted damage. */
    default void onDamageTakenConfirmed(@NotNull ArenaPlayer self, @Nullable ArenaPlayer attacker, @NotNull EntityDamageEvent event) {}

    /**
     * Called when the damage would kill the owner.
     *
     * @return {@code true} if the owner survived (the damage is cancelled)
     */
    default boolean onFatalDamage(@NotNull ArenaPlayer self, @NotNull EntityDamageEvent event) {
        return false;
    }

    /**
     * Called when the owner defeats a player.
     */
    default void onKill(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim) {}

    /**
     * Called for every owner in the arena when any player is defeated.
     */
    default void onPlayerDeath(@NotNull ArenaPlayer self, @NotNull ArenaPlayer victim, @Nullable ArenaPlayer killer) {}

    /**
     * Called after the owner successfully used a skill.
     */
    default void onSkillUse(@NotNull ArenaPlayer self, @NotNull Skill skill) {}

    /**
     * Returns whether the owner may regenerate health naturally.
     */
    default boolean canRegenerate(@NotNull ArenaPlayer self) {
        return true;
    }
}
