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

class FolkloreLuaTest {
    private static final Path ROOT = Path.of("src/main/resources/architects");

    @Test void tamatebakoWaitsForQuietCombatAndRespectsHealIntervalAndFullHealth() throws Exception {
        LoadedScript script = load("tamatebako.json");
        Fixture owner = new Fixture(script.definition);
        Fixture otherOwner = new Fixture(script.definition);
        int quiet = owner.param("quiet_ticks").toint();
        int interval = owner.param("interval").toint();
        double heal = owner.param("heal").todouble();

        owner.tick = 10;
        otherOwner.tick = 10;
        owner.run(script.lua, "on_equip");
        otherOwner.run(script.lua, "on_equip");
        owner.tick = 10 + quiet - 1;
        owner.run(script.lua, "on_hit", "TOOL");
        owner.tick++;
        owner.run(script.lua, "on_tick");
        assertEquals(0, owner.heals.size(), "a confirmed hit restarts quiet time");
        otherOwner.tick = 10 + quiet;
        otherOwner.run(script.lua, "on_tick");
        assertEquals(List.of(heal), otherOwner.heals, "one owner's combat must not delay another owner");

        owner.run(script.lua, "on_hit", "SECONDARY");
        owner.run(script.lua, "on_hurt", "ENVIRONMENT");
        owner.tick = 10 + 2 * quiet - 2;
        owner.run(script.lua, "on_hurt", "SKILL");
        owner.tick = 10 + 2 * quiet - 2 + quiet - 1;
        owner.run(script.lua, "on_tick");
        assertTrue(owner.heals.isEmpty(), "confirmed damage taken also restarts quiet time");
        owner.tick++;
        owner.run(script.lua, "on_tick");
        assertEquals(List.of(heal), owner.heals);
        assertEquals(owner.tick + interval, owner.numberState("ready"));

        int firstHealTick = owner.tick;
        owner.tick = firstHealTick + interval - 1;
        owner.run(script.lua, "on_tick");
        assertEquals(1, owner.heals.size());
        owner.tick++;
        owner.health = owner.maxHealth;
        owner.run(script.lua, "on_tick");
        assertEquals(1, owner.heals.size(), "full health must not consume the ready heal");
        owner.health--;
        owner.run(script.lua, "on_tick");
        assertEquals(List.of(heal, heal), owner.heals);
    }

    @Test void kudanTriggersAtThirtyPercentAndRearmsAtCooldownBoundary() throws Exception {
        LoadedScript script = load("kudan.json");
        Fixture owner = new Fixture(script.definition);
        Fixture otherOwner = new Fixture(script.definition);
        int interval = owner.param("interval").toint();
        owner.tick = 100;
        otherOwner.tick = 100;
        owner.run(script.lua, "on_equip");
        otherOwner.run(script.lua, "on_equip");
        double thresholdHealth = owner.maxHealth * owner.param("threshold").todouble();

        owner.health = thresholdHealth + 0.01;
        owner.run(script.lua, "on_tick");
        assertTrue(owner.buffs.isEmpty());
        owner.health = thresholdHealth;
        owner.run(script.lua, "on_tick");
        assertEquals(List.of(
                new Buff("speed", owner.param("speed_ticks").toint(), owner.param("speed_amplifier").toint(), 100 + owner.param("speed_ticks").toint()),
                new Buff("resistance", owner.param("resistance_ticks").toint(), owner.param("resistance_amplifier").toint(), 100 + owner.param("resistance_ticks").toint())), owner.buffs);
        assertEquals(100 + interval, owner.numberState("ready"));

        otherOwner.health = thresholdHealth;
        otherOwner.run(script.lua, "on_tick");
        assertEquals(2, otherOwner.buffs.size(), "cooldown belongs to each owner");
        owner.tick = 100 + interval - 1;
        owner.run(script.lua, "on_tick");
        assertEquals(2, owner.buffs.size());
        owner.tick++;
        owner.run(script.lua, "on_tick");
        assertEquals(4, owner.buffs.size(), "the next buff starts when the cooldown expires");
        assertEquals(owner.tick + owner.param("speed_ticks").toint(), owner.buffs.get(2).expiresAt());
        assertEquals(owner.tick + owner.param("resistance_ticks").toint(), owner.buffs.get(3).expiresAt());
    }

    @Test void issunBoostsAfterFourSecondsUntilAConfirmedHitAndIgnoresSecondaryDamage() throws Exception {
        LoadedScript script = load("issun.json");
        Fixture owner = new Fixture(script.definition);
        int window = owner.param("window").toint();
        double multiplier = owner.param("multiplier").todouble();
        owner.tick = 100;
        owner.run(script.lua, "on_equip");
        assertEquals(owner.param("health_penalty").todouble(), owner.attributes.get("health").doubleValue(), 0.0001);

        owner.tick = 100 + window - 1;
        owner.run(script.lua, "on_damage_dealt", "TOOL");
        assertTrue(owner.damageScales.isEmpty());
        owner.tick++;
        owner.run(script.lua, "on_damage_dealt", "AREA_TOOL");
        assertEquals(List.of(multiplier), owner.damageScales);
        owner.run(script.lua, "on_hit", "TOOL");
        assertEquals(owner.tick, owner.numberState("last_hit"));
        owner.tick += window - 1;
        owner.run(script.lua, "on_damage_dealt", "TOOL");
        assertEquals(1, owner.damageScales.size());
        owner.tick++;
        owner.run(script.lua, "on_hit", "SECONDARY");
        owner.run(script.lua, "on_damage_dealt", "SECONDARY");
        assertEquals(1, owner.damageScales.size());
        assertEquals(100 + window, owner.numberState("last_hit"), "secondary hits do not reset the clock");
        owner.run(script.lua, "on_damage_dealt", "SKILL");
        assertEquals(List.of(multiplier, multiplier), owner.damageScales);
    }

    @Test void shuraUsesFortyPercentThresholdAndAlwaysIncreasesIncomingDamage() throws Exception {
        LoadedScript script = load("shura.json");
        Fixture owner = new Fixture(script.definition);
        double thresholdHealth = owner.maxHealth * owner.param("threshold").todouble();
        double bonus = owner.param("damage_bonus").todouble();
        double taken = owner.param("damage_taken").todouble();

        owner.health = thresholdHealth + 0.01;
        owner.run(script.lua, "on_damage_dealt", "TOOL");
        assertTrue(owner.damageScales.isEmpty());
        owner.health = thresholdHealth;
        owner.run(script.lua, "on_damage_dealt", "AREA_TOOL");
        owner.run(script.lua, "on_damage_dealt", "SECONDARY");
        assertEquals(List.of(bonus), owner.damageScales);
        owner.run(script.lua, "on_damage_taken", "TOOL");
        owner.health = owner.maxHealth;
        owner.run(script.lua, "on_damage_taken", "SECONDARY");
        assertEquals(List.of(bonus, taken, taken), owner.damageScales);
    }

    private static LoadedScript load(String jsonName) throws Exception {
        ArchitectDefinition definition = ArchitectDefinition.parse(Files.readString(ROOT.resolve(jsonName)));
        LuaSandbox lua = new LuaSandbox(Files.readString(ROOT.resolve(definition.script())), definition.script());
        return new LoadedScript(definition, lua);
    }

    private record LoadedScript(ArchitectDefinition definition, LuaSandbox lua) {}
    private record Buff(String name, int duration, int amplifier, int expiresAt) {}

    /** Scalar-only GameplayContext substitute; state is private to each fixture owner. */
    private static final class Fixture {
        private final ArchitectDefinition definition;
        private final Map<String, LuaValue> state = new HashMap<>();
        private final List<Double> heals = new ArrayList<>();
        private final List<Buff> buffs = new ArrayList<>();
        private final List<Double> damageScales = new ArrayList<>();
        private final Map<String, Double> attributes = new HashMap<>();
        private int tick;
        private double health = 15;
        private final double maxHealth = 20;

        private Fixture(ArchitectDefinition definition) { this.definition = definition; }

        private LuaValue param(String name) { return LuaValue.valueOf(definition.parameters().get(name)); }
        private int numberState(String name) { return state.getOrDefault(name, LuaValue.ZERO).toint(); }

        private void run(LuaSandbox lua, String hook) { run(lua, hook, "TOOL"); }
        private void run(LuaSandbox lua, String hook, String damageKind) {
            LuaTable context = new LuaTable();
            LuaTable params = new LuaTable();
            definition.parameters().forEach((key, value) -> params.set(key, LuaValue.valueOf(value)));
            context.set("params", params);
            context.set("tick", LuaValue.valueOf(tick));
            context.set("health", LuaValue.valueOf(health));
            context.set("max_health", LuaValue.valueOf(maxHealth));
            context.set("damage_kind", LuaValue.valueOf(damageKind));
            context.set("state", function(args -> state.getOrDefault(args.checkjstring(1), args.arg(2))));
            context.set("set_state", function(args -> {
                String key = args.checkjstring(1);
                LuaValue value = args.arg(2);
                if (value.isnil()) state.remove(key); else state.put(key, value);
                return LuaValue.NIL;
            }));
            context.set("heal", function(args -> {
                heals.add(args.checkdouble(1));
                return LuaValue.TRUE;
            }));
            context.set("buff", function(args -> {
                int duration = args.checkint(2);
                buffs.add(new Buff(args.checkjstring(1), duration, args.checkint(3), tick + duration));
                return LuaValue.NIL;
            }));
            context.set("scale_damage", function(args -> {
                damageScales.add(args.checkdouble(1));
                return LuaValue.NIL;
            }));
            context.set("attribute", function(args -> {
                attributes.put(args.checkjstring(1), args.checkdouble(2));
                return LuaValue.NIL;
            }));
            context.set("notice", function(args -> LuaValue.NIL));
            context.set("effect", function(args -> LuaValue.NIL));
            lua.invoke(hook, context);
        }

        private static VarArgFunction function(Function<Varargs, LuaValue> body) {
            return new VarArgFunction() {
                @Override public Varargs invoke(Varargs args) { return body.apply(args); }
            };
        }
    }
}
