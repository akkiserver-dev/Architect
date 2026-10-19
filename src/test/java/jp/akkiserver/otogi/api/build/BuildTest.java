package jp.akkiserver.otogi.api.build;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Park;
import jp.akkiserver.otogi.core.architect.BaseArchitect;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class BuildTest {
    private static final class TestPark extends BaseArchitect implements Park {
        private final Set<NamespacedKey> blocked;
        TestPark(String id, Set<NamespacedKey> blocked) { super(id, id); this.blocked = blocked; }
        @Override public Set<NamespacedKey> getIncompatiblearchitects() { return blocked; }
    }

    @AfterEach void clearRegistry() { OtogiAPI.shutdown(); }

    @Test void rejectsSecondParkAndDuplicates() {
        var first = new TestPark("first", Set.of());
        var second = new TestPark("second", Set.of());
        OtogiAPI.registerArchitects(first, second);
        Build build = new Build();
        assertNull(Builds.add(build, first));
        assertNotNull(Builds.add(build, first));
        assertNotNull(Builds.add(build, second));
        assertEquals(1, build.size());
    }

    @Test void incompatibilityIsSymmetricRegardlessOfSelectionOrder() {
        var tool = new BaseArchitect("tool", "具") {};
        var park = new TestPark("park", Set.of(tool.getId()));
        OtogiAPI.registerArchitects(tool, park);
        Build build = new Build();
        assertNull(Builds.add(build, tool));
        assertNotNull(Builds.add(build, park));
        build.clear();
        assertNull(Builds.add(build, park));
        assertNotNull(Builds.add(build, tool));
    }

    @Test void refusesOverflowWithoutChangingSelectedOrder() {
        Build build = new Build();
        for (int i = 0; i < Builds.DEFAULT_SIZE; i++) {
            var architect = new BaseArchitect("choice_" + i, "選択") {};
            OtogiAPI.registerArchitect(architect);
            assertNull(Builds.add(build, architect));
        }
        assertNotNull(Builds.add(build, new BaseArchitect("overflow", "枠外") {}));
        assertEquals(Builds.DEFAULT_SIZE, build.size());
        assertEquals("choice_0", build.getArchitects().getFirst().getId().getKey());
    }
}
