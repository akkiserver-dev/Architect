package jp.akkiserver.otogi.core.script;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import static org.junit.jupiter.api.Assertions.*;

class ScriptStateTest {
    @Test void playersAndAbilitiesStayIsolatedAndNewLivesStartEmpty() {
        ArenaPlayer first = new ArenaPlayer(null, null);
        ArenaPlayer second = new ArenaPlayer(null, null);
        ScriptState.of(first, "script.one").set("ready", LuaValue.valueOf(100));
        assertEquals(100, ScriptState.of(first, "script.one").get("ready", LuaValue.NIL).toint());
        assertEquals(0, ScriptState.of(second, "script.one").get("ready", LuaValue.NIL).toint());
        assertEquals(0, ScriptState.of(first, "script.two").get("ready", LuaValue.NIL).toint());
        first.resetCombatState();
        assertEquals(0, ScriptState.of(first, "script.one").get("ready", LuaValue.NIL).toint());
    }

    @Test void rejectsTablesNonFiniteNumbersAndOversizedStateButAllowsDeletion() {
        ScriptState state = new ScriptState();
        assertThrows(LuaError.class, () -> state.set("table", new LuaTable()));
        assertThrows(LuaError.class, () -> state.set("number", LuaValue.valueOf(Double.NaN)));
        assertThrows(LuaError.class, () -> state.set("message", LuaValue.valueOf("x".repeat(257))));
        for (int i = 0; i < 64; i++) state.set("key" + i, LuaValue.TRUE);
        assertThrows(LuaError.class, () -> state.set("overflow", LuaValue.TRUE));
        state.set("key0", LuaValue.NIL);
        state.set("replacement", LuaValue.FALSE);
        assertFalse(state.get("replacement", LuaValue.TRUE).toboolean());
    }
}
