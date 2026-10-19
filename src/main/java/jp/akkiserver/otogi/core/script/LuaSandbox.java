package jp.akkiserver.otogi.core.script;

import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.DebugLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/** Small expressions and gameplay composition only. No Java objects ever cross this boundary. */
public final class LuaSandbox {
    public static final Set<String> HOOKS = Set.of("on_use", "on_attack", "on_equip", "on_unequip",
            "on_tick", "on_damage_dealt", "on_damage_taken", "on_hit", "on_hurt", "on_kill", "on_skill");
    private final Prototype prototype;
    private final Set<String> hooks;

    public LuaSandbox(String source, String name) {
        if (source.getBytes(StandardCharsets.UTF_8).length > 32_768) throw new IllegalArgumentException("Lua exceeds 32 KiB");
        try {
            prototype = LuaC.instance.compile(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)), name);
            inspect(prototype, 0);
            LuaTable table = instantiate(newGlobals()).checktable();
            java.util.HashSet<String> found = new java.util.HashSet<>();
            for (LuaValue key : table.keys()) {
                if (!key.isstring() || !HOOKS.contains(key.tojstring()) || !table.get(key).isfunction())
                    throw new IllegalArgumentException("Unknown hook or non-function: " + key.tojstring());
                found.add(key.tojstring());
            }
            if (found.isEmpty()) throw new IllegalArgumentException("Lua must return a table of hooks");
            hooks = Set.copyOf(found);
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Cannot compile " + name, exception);
        }
    }

    public boolean hasHook(String name) { return hooks.contains(name); }

    public LuaValue invoke(String hook, LuaTable context) {
        if (!hasHook(hook)) return LuaValue.NIL;
        Globals globals = newGlobals();
        return instantiate(globals).get(hook).call(context);
    }

    private LuaValue instantiate(Globals globals) { return new LuaClosure(prototype, globals).call(); }

    private static Globals newGlobals() {
        // Build a whitelist instead of standardGlobals(), which includes OS, IO and Java facilities.
        Globals library = new Globals();
        library.load(new BaseLib());
        // MathLib registers itself here. This private staging table is never supplied to a script.
        LuaTable loaded = new LuaTable();
        LuaTable packageTable = new LuaTable();
        packageTable.set("loaded", loaded);
        library.set("package", packageTable);
        library.load(new JseMathLib());
        Globals result = new Globals();
        for (String name : new String[]{"assert", "error", "ipairs", "pairs", "next", "tonumber", "tostring", "type", "select"})
            result.set(name, library.get(name));
        LuaTable math = new LuaTable();
        for (String name : new String[]{"abs", "ceil", "floor", "min", "max", "sqrt", "sin", "cos", "pi"})
            math.set(name, library.get("math").get(name));
        result.set("math", math);
        result.debuglib = new Budget();
        return result;
    }

    private static void inspect(Prototype prototype, int depth) {
        if (depth > 24) throw new IllegalArgumentException("Lua nesting exceeds 24");
        for (int instruction : prototype.code) {
            // Exponential string allocation is not covered by an instruction quota. Static strings suffice here.
            if (Lua.GET_OPCODE(instruction) == Lua.OP_CONCAT)
                throw new IllegalArgumentException("Lua string concatenation is disabled; use static messages");
        }
        for (Prototype child : prototype.p) inspect(child, depth + 1);
    }

    private static final class Budget extends DebugLib {
        private int remaining = 10_000;
        private int depth;
        @Override public void onInstruction(int pc, Varargs args, int top) {
            if (--remaining <= 0) throw new LuaError("Lua instruction budget exceeded (10000)");
        }
        @Override public void onCall(LuaFunction function) { enter(); }
        @Override public void onCall(LuaClosure closure, Varargs args, LuaValue[] stack) { enter(); }
        private void enter() { if (++depth > 48) throw new LuaError("Lua call depth exceeded (48)"); }
        @Override public void onReturn() { depth--; }
        @Override public String traceback(int level) { return "[restricted gameplay script]"; }
    }
}
