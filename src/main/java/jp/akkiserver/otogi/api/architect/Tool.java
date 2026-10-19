package jp.akkiserver.otogi.api.architect;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

/**
 * A weapon architect (具). Its item is always carried by the player.
 * <p>
 * Vanilla melee damage is never used. A left click calls {@link #onAttack(ArenaPlayer)} and the tool
 * decides its own hit detection, effects and damage.
 */
public interface Tool extends Architect {
    @NotNull
    ItemStack createItem();

    /**
     * Gets the minimum ticks between two attacks.
     */
    int getAttackInterval();

    /**
     * Performs the attack of this tool.
     *
     * @return {@code true} if the attack was performed and the attack interval should start
     */
    boolean onAttack(@NotNull ArenaPlayer player);

    /** Optional right-click action. The tool owns its cooldown. */
    default boolean onUse(@NotNull ArenaPlayer player) { return false; }
}
