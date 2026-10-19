package jp.akkiserver.otogi.api.architect;

public interface CoolTime {
    /**
     * Gets the default cooldown duration in ticks.
     */
    int getDefaultCoolTime();

    /**
     * Gets remaining cooldown duration in ticks.
     */
    int getRemainingCoolTime();

    /**
     * Returns whether this is currently cooling down.
     */
    default boolean isCooldown() {
        return getRemainingCoolTime() > 0;
    }

    /**
     * Returns whether this can be activated.
     */
    default boolean isReady() {
        return !isCooldown();
    }
}
