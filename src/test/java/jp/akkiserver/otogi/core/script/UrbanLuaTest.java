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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class UrbanLuaTest {
    private static final Path ROOT = Path.of("src/main/resources/architects");

    @Test void kuchisakeEarnsPreciseFollowupAndSpendsItBeforeAWhiff() throws Exception {
        Loaded tool = load("kuchisake.json");
        Fixture owner = new Fixture(tool.definition);
        owner.tick = 100;
        assertEquals(18, tool.definition.attackInterval());
        assertEquals(120, tool.definition.cooldown());
        owner.castHits = 0;
        assertTrue(owner.run(tool.lua, "on_attack").toboolean());
        assertEquals(new Cast("cone", 3, 35, 2.8, "AREA_TOOL", 0), owner.casts.getLast());
        assertFalse(owner.use(tool.lua));

        owner.castHits = 1;
        assertTrue(owner.run(tool.lua, "on_attack").toboolean());
        assertEquals(140, owner.stateInt("ready_until"));
        owner.tick = 139;
        owner.castHits = 0;
        assertTrue(owner.use(tool.lua), "a missed follow-up still fires and spends cooldown");
        assertEquals(new Cast("first", 6, .18, 4.5, "TOOL", .2), owner.casts.getLast());
        assertEquals(0, owner.stateInt("ready_until"));
        assertEquals(259, owner.nextUse);
        assertFalse(owner.use(tool.lua));
        assertEquals(3, owner.casts.size(), "no extra cast during cooldown");

        owner.tick = 259;
        assertFalse(owner.use(tool.lua), "a spent opportunity cannot be replayed at cooldown expiry");
        owner.castHits = 1;
        owner.run(tool.lua, "on_attack");
        owner.tick = 299;
        assertFalse(owner.use(tool.lua), "the window closes at the exact end tick");
        owner.run(tool.lua, "on_attack");
        assertTrue(owner.use(tool.lua));
    }

    @Test void kuchisakeReadinessBelongsToItsOwnerAndResetsOnEquip() throws Exception {
        Loaded tool = load("kuchisake.json");
        Fixture first = new Fixture(tool.definition);
        Fixture second = new Fixture(tool.definition);
        first.tick = second.tick = 40;
        first.castHits = 1;
        first.run(tool.lua, "on_attack");
        assertFalse(second.use(tool.lua));
        assertEquals(80, first.stateInt("ready_until"));
        first.clearLife();
        assertFalse(first.use(tool.lua));
    }

    @Test void okuriinuCountsOnlyConfirmedToolHurtAndKeepsRollingWindow() throws Exception {
        Loaded passive = load("okuriinu.json");
        Fixture owner = new Fixture(passive.definition);
        Fixture other = new Fixture(passive.definition);
        owner.tick = other.tick = 10;
        owner.run(passive.lua, "on_equip");
        other.run(passive.lua, "on_equip");
        for (String kind : List.of("SECONDARY", "SKILL", "AREA_SKILL", "ENVIRONMENT"))
            owner.hurt(passive.lua, kind);
        assertEquals(0, owner.stateInt("hit_count"));

        owner.hurt(passive.lua, "TOOL");
        owner.tick = 50;
        owner.hurt(passive.lua, "AREA_TOOL");
        assertEquals(2, owner.stateInt("hit_count"));
        owner.tick = 110;
        owner.hurt(passive.lua, "TOOL");
        assertEquals(2, owner.stateInt("hit_count"), "tick 10 expires but tick 50 remains in the window");
        assertEquals(50, owner.stateInt("first_hit"));
        assertTrue(owner.buffs.isEmpty());
        owner.tick = 149;
        owner.hurt(passive.lua, "AREA_TOOL");
        assertEquals(List.of(new Buff("resistance", 40, 0), new Buff("speed", 40, 0)), owner.buffs);
        assertEquals(0, owner.stateInt("hit_count"));
        assertEquals(349, owner.stateInt("ready"));
        assertTrue(other.buffs.isEmpty());
        assertEquals(0, other.stateInt("hit_count"));
        other.tick = 149;
        other.hurt(passive.lua, "TOOL");
        other.hurt(passive.lua, "TOOL");
        other.hurt(passive.lua, "AREA_TOOL");
        assertEquals(2, other.buffs.size(), "another owner can trigger independently");
        assertEquals(349, owner.stateInt("ready"));
    }

    @Test void okuriinuCooldownIgnoresHitsAndReopensAtExactTick() throws Exception {
        Loaded passive = load("okuriinu.json");
        Fixture owner = new Fixture(passive.definition);
        owner.tick = 20;
        owner.run(passive.lua, "on_equip");
        owner.hurt(passive.lua, "TOOL");
        owner.hurt(passive.lua, "AREA_TOOL");
        owner.hurt(passive.lua, "TOOL");
        assertEquals(220, owner.stateInt("ready"));
        owner.tick = 219;
        owner.hurt(passive.lua, "TOOL");
        assertEquals(0, owner.stateInt("hit_count"));
        owner.tick = 220;
        owner.hurt(passive.lua, "TOOL");
        assertEquals(1, owner.stateInt("hit_count"));
        owner.tick = 320;
        owner.run(passive.lua, "on_tick");
        assertEquals(0, owner.stateInt("hit_count"), "expired stacks are cleared without a new hit");
        owner.hurt(passive.lua, "TOOL");
        assertEquals(1, owner.stateInt("hit_count"));
        owner.clearLife();
        owner.run(passive.lua, "on_equip");
        assertEquals(0, owner.stateInt("ready"));
        assertEquals(0, owner.stateInt("hit_count"));
    }

    private static Loaded load(String file) throws Exception {
        ArchitectDefinition definition = ArchitectDefinition.parse(Files.readString(ROOT.resolve(file)));
        return new Loaded(definition, new LuaSandbox(Files.readString(ROOT.resolve(definition.script())), definition.script()));
    }

    private record Loaded(ArchitectDefinition definition, LuaSandbox lua) {}
    private record Cast(String shape, double range, double width, double damage, String kind, double knockback) {}
    private record Buff(String name, int duration, int amplifier) {}

    /** Only scalar ctx values cross the Lua boundary; each fixture represents one owner and life. */
    private static final class Fixture {
        private final ArchitectDefinition definition;
        private final Map<String, LuaValue> state = new HashMap<>();
        private final List<Cast> casts = new ArrayList<>();
        private final List<Buff> buffs = new ArrayList<>();
        private int tick;
        private int castHits;
        private int nextUse;

        private Fixture(ArchitectDefinition definition) { this.definition = definition; }
        private int stateInt(String key) { return state.getOrDefault(key, LuaValue.ZERO).toint(); }
        private void clearLife() { state.clear(); }
        private void hurt(LuaSandbox lua, String kind) { run(lua, "on_hurt", kind); }
        private boolean use(LuaSandbox lua) {
            if (tick < nextUse) return false;
            boolean used = run(lua, "on_use").toboolean();
            if (used) nextUse = tick + definition.cooldown();
            return used;
        }
        private LuaValue run(LuaSandbox lua, String hook) { return run(lua, hook, "TOOL"); }
        private LuaValue run(LuaSandbox lua, String hook, String kind) {
            LuaTable ctx = new LuaTable();
            LuaTable params = new LuaTable();
            definition.parameters().forEach((key, value) -> params.set(key, LuaValue.valueOf(value)));
            ctx.set("params", params);
            ctx.set("tick", LuaValue.valueOf(tick));
            ctx.set("damage_kind", LuaValue.valueOf(kind));
            ctx.set("state", function(args -> state.getOrDefault(args.checkjstring(1), args.arg(2))));
            ctx.set("set_state", function(args -> {
                String key = args.checkjstring(1);
                LuaValue value = args.arg(2);
                if (value.isnil()) state.remove(key); else state.put(key, value);
                return LuaValue.NIL;
            }));
            ctx.set("cast", function(args -> {
                casts.add(new Cast(args.checkjstring(1), args.checkdouble(2), args.checkdouble(3),
                        args.checkdouble(4), args.checkjstring(5), args.checkdouble(6)));
                return LuaValue.valueOf(castHits);
            }));
            ctx.set("buff", function(args -> {
                buffs.add(new Buff(args.checkjstring(1), args.checkint(2), args.checkint(3)));
                return LuaValue.NIL;
            }));
            ctx.set("notice", function(args -> LuaValue.NIL));
            ctx.set("effect", function(args -> LuaValue.NIL));
            return lua.invoke(hook, ctx);
        }
        private static VarArgFunction function(Function<Varargs, LuaValue> body) {
            return new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) { return body.apply(args); }
            };
        }
    }
}
