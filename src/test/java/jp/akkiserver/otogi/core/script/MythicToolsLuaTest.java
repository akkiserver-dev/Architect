package jp.akkiserver.otogi.core.script;

import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MythicToolsLuaTest {
    private static final Path ROOT = Path.of("src/main/resources/architects");

    @Test void orochiMissDoesNotChargeAndThreeFangBurstSpendsItsDamage() throws Exception {
        LoadedTool orochi = load("orochi.json");
        Fixture owner = new Fixture(orochi.definition);

        owner.castHits = 0;
        assertTrue(orochi.lua.invoke("on_attack", owner.context()).toboolean());
        assertEquals(0, owner.numberState("fangs"));
        assertFalse(orochi.lua.invoke("on_use", owner.context()).toboolean());

        owner.castHits = 1;
        for (int i = 0; i < 4; i++) assertTrue(orochi.lua.invoke("on_attack", owner.context()).toboolean());
        assertEquals(3, owner.numberState("fangs"), "fangs stop at the configured maximum");

        owner.casts.clear();
        assertTrue(orochi.lua.invoke("on_use", owner.context()).toboolean());
        assertEquals(1, owner.casts.size());
        Cast burst = owner.casts.getFirst();
        assertEquals("sphere", burst.shape);
        assertEquals("AREA_TOOL", burst.kind);
        assertEquals(5.5, burst.damage, 0.0001, "base 1 plus three fangs at 1.5 damage each");
        assertEquals(0, owner.numberState("fangs"), "a successful burst spends every fang");
    }

    @Test void orochiFangStateIsIsolatedBetweenOwners() throws Exception {
        LoadedTool orochi = load("orochi.json");
        Fixture firstOwner = new Fixture(orochi.definition);
        Fixture secondOwner = new Fixture(orochi.definition);
        firstOwner.castHits = 1;

        assertTrue(orochi.lua.invoke("on_attack", firstOwner.context()).toboolean());
        assertEquals(1, firstOwner.numberState("fangs"));
        assertEquals(0, secondOwner.numberState("fangs"));
        assertFalse(orochi.lua.invoke("on_use", secondOwner.context()).toboolean());
        assertTrue(orochi.lua.invoke("on_use", firstOwner.context()).toboolean());
        assertEquals(0, firstOwner.numberState("fangs"));
        assertEquals(0, secondOwner.numberState("fangs"));
    }

    @Test void inabaRequiresARecentHitAndReadinessExpiresAtTheExactBoundary() throws Exception {
        LoadedTool inaba = load("inaba.json");
        Fixture owner = new Fixture(inaba.definition);
        owner.tick = 100;
        owner.castHits = 0;
        assertTrue(inaba.lua.invoke("on_attack", owner.context()).toboolean());
        assertEquals(0, owner.numberState("ready_until"));
        assertFalse(inaba.lua.invoke("on_use", owner.context()).toboolean());

        owner.castHits = 1;
        assertTrue(inaba.lua.invoke("on_attack", owner.context()).toboolean());
        assertEquals(160, owner.numberState("ready_until"));
        owner.tick = 130;
        owner.dashSucceeds = false;
        assertFalse(inaba.lua.invoke("on_use", owner.context()).toboolean());
        assertEquals(160, owner.numberState("ready_until"), "failed movement keeps the earned jump");

        owner.tick = 140;
        owner.dashSucceeds = true;
        assertTrue(inaba.lua.invoke("on_use", owner.context()).toboolean());
        assertEquals(0, owner.numberState("ready_until"), "a successful jump consumes readiness");
        assertEquals(2, owner.dashCalls, "one failed attempt and one successful jump");

        owner.tick = 200;
        assertTrue(inaba.lua.invoke("on_attack", owner.context()).toboolean());
        assertEquals(260, owner.numberState("ready_until"));
        owner.tick = 260;
        assertFalse(inaba.lua.invoke("on_use", owner.context()).toboolean());
        assertEquals(260, owner.numberState("ready_until"), "the window is closed when tick == ready_until");
        assertEquals(2, owner.dashCalls, "an expired window does not attempt movement");
    }

    private static LoadedTool load(String jsonName) throws Exception {
        ArchitectDefinition definition = ArchitectDefinition.parse(Files.readString(ROOT.resolve(jsonName)));
        LuaSandbox lua = new LuaSandbox(Files.readString(ROOT.resolve(definition.script())), definition.script());
        return new LoadedTool(definition, lua);
    }

    private record LoadedTool(ArchitectDefinition definition, LuaSandbox lua) {}
    private record Cast(String shape, String kind, double damage) {}

    /** Minimal scalar-only stand-in for GameplayContext; each instance represents one owner. */
    private static final class Fixture {
        private final ArchitectDefinition definition;
        private final Map<String, LuaValue> state = new HashMap<>();
        private final List<Cast> casts = new ArrayList<>();
        private int tick;
        private int castHits;
        private int dashCalls;
        private boolean dashSucceeds = true;

        private Fixture(ArchitectDefinition definition) { this.definition = definition; }

        private LuaTable context() {
            LuaTable context = new LuaTable();
            LuaTable params = new LuaTable();
            definition.parameters().forEach((key, value) -> params.set(key, LuaValue.valueOf(value)));
            context.set("params", params);
            context.set("tick", LuaValue.valueOf(tick));
            context.set("state", function(args -> state.getOrDefault(args.checkjstring(1), args.arg(2))));
            context.set("set_state", function(args -> {
                String key = args.checkjstring(1);
                LuaValue value = args.arg(2);
                if (value.isnil()) state.remove(key); else state.put(key, value);
                return LuaValue.NIL;
            }));
            context.set("cast", function(args -> {
                casts.add(new Cast(args.checkjstring(1), args.checkjstring(5), args.checkdouble(4)));
                return LuaValue.valueOf(castHits);
            }));
            context.set("dash", function(args -> {
                dashCalls++;
                return LuaValue.valueOf(dashSucceeds);
            }));
            context.set("notice", function(args -> LuaValue.NIL));
            context.set("effect", function(args -> LuaValue.NIL));
            return context;
        }

        private int numberState(String key) {
            LuaValue value = state.get(key);
            return value == null ? 0 : value.toint();
        }

        private static VarArgFunction function(java.util.function.Function<Varargs, LuaValue> body) {
            return new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) { return body.apply(args); }
            };
        }
    }
}
