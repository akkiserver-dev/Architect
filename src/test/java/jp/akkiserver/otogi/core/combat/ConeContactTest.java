package jp.akkiserver.otogi.core.combat;

import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConeContactTest {
    private final Vector eye = new Vector(0, 1.6, 0);
    private final Vector forward = new Vector(0, 0, 1);

    @Test void closeTargetsBehindThePlayerAreNotAutomaticallyHit() {
        assertNull(CombatGeometry.coneContact(new BoundingBox(-.3, 0, -.7, .3, 1.8, -.1), eye, forward, 3, 55));
    }
    @Test void frontSurfaceCountsEvenWhenTheCenterIsOutOfReach() {
        Vector contact = CombatGeometry.coneContact(new BoundingBox(-.3, 0, 2.9, .3, 1.8, 3.5), eye, forward, 3, 55);
        assertNotNull(contact);
        assertEquals(2.9, contact.getZ(), 1e-7);
    }
    @Test void bodyEdgeCountsWhenItsCenterLiesOutsideTheCone() {
        assertNotNull(CombatGeometry.coneContact(new BoundingBox(1.4, 0, 1.5, 2, 1.8, 2.1), eye, forward, 3, 40));
    }
    @Test void aimPitchChangesTheConeInsteadOfLeavingAHorizontalHitbox() {
        BoundingBox below = new BoundingBox(-.3, -2, 1, .3, -.2, 1.6);
        assertNull(CombatGeometry.coneContact(below, eye, forward, 3, 20));
        assertNotNull(CombatGeometry.coneContact(below, eye, new Vector(0, -1, .6), 3, 20));
    }
    @Test void invalidDirectionsAndOutOfRangeTargetsNeverHit() {
        BoundingBox box = new BoundingBox(-.3, 0, 3.1, .3, 1.8, 3.7);
        assertNull(CombatGeometry.coneContact(box, eye, forward, 3, 55));
        assertNull(CombatGeometry.coneContact(box, eye, new Vector(), 3, 55));
        assertNull(CombatGeometry.coneContact(box, eye, forward, Double.NaN, 55));
    }
}
