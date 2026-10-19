package jp.akkiserver.otogi.core.script;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;

import java.util.HashMap;
import java.util.Map;

/** Bounded scalar state lives in ArenaPlayer so resetCombatState and restoreState remove it. */
final class ScriptState {
    private final Map<String, LuaValue> values = new HashMap<>();

    static ScriptState of(ArenaPlayer self, String ownerKey) {
        ScriptState state = self.getState(ownerKey, ScriptState.class);
        if (state == null) { state = new ScriptState(); self.setState(ownerKey, state); }
        return state;
    }

    LuaValue get(String key, LuaValue fallback) {
        validateKey(key);
        return values.getOrDefault(key, fallback.isnil() ? LuaValue.ZERO : scalar(fallback));
    }

    void set(String key, LuaValue value) {
        validateKey(key);
        if (value.isnil()) { values.remove(key); return; }
        if (!values.containsKey(key) && values.size() >= 64) throw new LuaError("At most 64 state keys");
        values.put(key, scalar(value));
    }

    private static void validateKey(String key) {
        if (!key.matches("[a-zA-Z0-9_]{1,48}")) throw new LuaError("Invalid state key");
    }

    private static LuaValue scalar(LuaValue value) {
        if (value.type() == LuaValue.TBOOLEAN) return value;
        if (value.type() == LuaValue.TNUMBER && Double.isFinite(value.todouble()) && Math.abs(value.todouble()) < 1e12) return value;
        if (value.type() == LuaValue.TSTRING && value.tojstring().length() <= 256) return value;
        throw new LuaError("State accepts only finite numbers, booleans and short strings");
    }
}
