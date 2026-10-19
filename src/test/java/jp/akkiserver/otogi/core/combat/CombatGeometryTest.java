package jp.akkiserver.otogi.core.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatGeometryTest {
    @Test
    void rearConeUsesHorizontalAnglesAndIncludesBoundary() {
        assertTrue(CombatGeometry.isBehind(0, 1, 0, -4, 45));
        assertTrue(CombatGeometry.isBehind(0, 1, 1, -1, 45));
        assertTrue(CombatGeometry.isBehind(0, 1, -1, -1, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 1.01, -1, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 0, 1, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 1, 0, 45));
    }

    @Test
    void rearConeDoesNotDependOnVectorMagnitude() {
        assertTrue(CombatGeometry.isBehind(200, 0, -0.002, 0, 45));
        assertFalse(CombatGeometry.isBehind(200, 0, 0.002, 0, 45));
    }

    @Test
    void degenerateOrNonFiniteDirectionsCannotBackstab() {
        assertFalse(CombatGeometry.isBehind(0, 0, 0, -1, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 0, 0, 45));
        assertFalse(CombatGeometry.isBehind(Double.NaN, 1, 0, -1, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 0, Double.POSITIVE_INFINITY, 45));
        assertFalse(CombatGeometry.isBehind(0, 1, 0, -1, Double.NaN));
    }

    @Test
    void tuningRejectsNonFiniteNumbersAndBoundsExtremeValues() {
        assertEquals(7, CombatGeometry.clampFinite(Double.NaN, 7, 0.05, 40));
        assertEquals(7, CombatGeometry.clampFinite(Double.POSITIVE_INFINITY, 7, 0.05, 40));
        assertEquals(0.05, CombatGeometry.clampFinite(-100, 7, 0.05, 40));
        assertEquals(40, CombatGeometry.clampFinite(1000, 7, 0.05, 40));
        assertEquals(12, CombatGeometry.clampFinite(12, 7, 0.05, 40));
    }
}
