package jp.akkiserver.otogi.api.arena;

import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import static org.junit.jupiter.api.Assertions.*;

class ArenaPlayerTest {
    private final BaseArchitect skill = new BaseArchitect("test", "試験") {};

    @Test void fractionalCooldownRatesDoNotFreezeAtOneTickUpdates() {
        ArenaPlayer player = new ArenaPlayer(null, null);
        player.setCoolTime(skill, 20);
        player.setCoolTimeRate(0.25);
        for (int tick = 0; tick < 79; tick++) player.tickCoolTimes(1);
        assertEquals(1, player.getRemainingCoolTime(skill));
        player.tickCoolTimes(1);
        assertEquals(0, player.getRemainingCoolTime(skill));
    }

    @Test void nextLifeClearsCooldownsAndTransientEffects() {
        ArenaPlayer player = new ArenaPlayer(null, null);
        player.setCoolTime(skill, 200);
        player.setCoolTimeRate(2);
        player.setState("ability.life.test", new Object());
        player.setMatchState("resurrection.used", true);
        player.resetCombatState();
        assertEquals(0, player.getRemainingCoolTime(skill));
        assertFalse(player.hasState("ability.life.test"));
        assertEquals(true, player.getMatchState("resurrection.used", Boolean.class));
        assertEquals(1, player.getCoolTimeRate());
    }

    @Test void invalidRateDoesNotPoisonCooldowns() {
        ArenaPlayer player = new ArenaPlayer(null, null);
        player.setCoolTime(skill, 1);
        player.setCoolTimeRate(Double.NaN);
        player.tickCoolTimes(1);
        assertEquals(0, player.getRemainingCoolTime(skill));
    }
}
