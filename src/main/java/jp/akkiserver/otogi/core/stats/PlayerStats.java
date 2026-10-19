package jp.akkiserver.otogi.core.stats;

/** Lifetime statistics for one player. */
public record PlayerStats(int kills, int deaths, int matches, int wins) {
    public double getKillDeathRatio() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }
}
