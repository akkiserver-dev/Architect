package jp.akkiserver.otogi.core.script;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.VarArgFunction;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Modifiers;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.AbilitySupport;
import jp.akkiserver.otogi.core.combat.CombatEffects;

import java.util.List;
import java.util.function.Function;

/** Typed scalar-only facade. All expensive target iteration and effects stay in Java. */
final class GameplayContext {
    private final ScriptArchitect owner;
    private final ArenaPlayer self;
    private final String hook;
    private final EntityDamageEvent event;
    private final int generation;
    private int calls;

    GameplayContext(ScriptArchitect owner, ArenaPlayer self, String hook, EntityDamageEvent event) {
        this.owner = owner;
        this.self = self;
        this.hook = hook;
        this.event = event;
        this.generation = self.getLifeGeneration();
    }

    LuaTable table() {
        LuaTable context = new LuaTable();
        LuaTable parameters = new LuaTable();
        owner.definition.parameters().forEach((key, value) -> parameters.set(key, LuaValue.valueOf(value)));
        context.set("params", parameters);
        context.set("tick", LuaValue.valueOf(Bukkit.getCurrentTick()));
        context.set("health", LuaValue.valueOf(self.getPlayer().getHealth()));
        context.set("max_health", LuaValue.valueOf(self.getMaxHealth()));
        context.set("damage", LuaValue.valueOf(event == null ? 0 : event.getDamage()));
        context.set("damage_kind", LuaValue.valueOf(Combat.isDealing() ? Combat.getDamageKind().name() : "ENVIRONMENT"));
        bind(context, "state", args -> {
            if (!valid()) return args.arg(2).isnil() ? LuaValue.ZERO : args.arg(2);
            return state().get(args.checkjstring(1), args.arg(2));
        });
        bind(context, "set_state", args -> {
            if (valid()) state().set(args.checkjstring(1), args.arg(2));
            return LuaValue.NIL;
        });
        bind(context, "heal", args -> LuaValue.valueOf(heal(number(args, 1, 0, 40))));
        bind(context, "dash", args -> LuaValue.valueOf(dash(number(args, 1, 0, 2), number(args, 2, -0.5, 1))));
        bind(context, "cast", args -> LuaValue.valueOf(cast(args.checkjstring(1), number(args, 2, .1, 32),
                number(args, 3, 0, 90), number(args, 4, 0, 40), args.checkjstring(5), number(args, 6, 0, 2))));
        bind(context, "effect", args -> { effect(args.checkjstring(1)); return LuaValue.NIL; });
        bind(context, "buff", args -> {
            if (valid()) self.getPlayer().addPotionEffect(new PotionEffect(potion(args.checkjstring(1)),
                    (int) number(args, 2, 1, 1200), (int) number(args, 3, 0, 4), false, true, true));
            return LuaValue.NIL;
        });
        bind(context, "attribute", args -> {
            if (!hook.equals("on_equip")) throw new LuaError("attribute is only available in on_equip");
            Attribute attribute = switch (args.checkjstring(1)) {
                case "speed" -> Attribute.MOVEMENT_SPEED;
                case "health" -> Attribute.MAX_HEALTH;
                case "gravity" -> Attribute.GRAVITY;
                default -> throw new LuaError("attribute must be speed, health or gravity");
            };
            double amount = number(args, 2, -.5, 1);
            if (valid()) Modifiers.add(self.getPlayer(), attribute, owner, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
            return LuaValue.NIL;
        });
        bind(context, "scale_damage", args -> {
            if (event == null || (!hook.equals("on_damage_dealt") && !hook.equals("on_damage_taken")))
                throw new LuaError("scale_damage requires a damage modifier hook");
            double multiplier = number(args, 1, 0, 3);
            if (valid()) event.setDamage(Math.clamp(event.getDamage() * multiplier, 0, 100));
            return LuaValue.NIL;
        });
        bind(context, "reduce_cooldowns", args -> { if (valid()) self.reduceSkillCoolTimes((int) number(args, 1, 0, 200)); return LuaValue.NIL; });
        bind(context, "notice", args -> {
            String text = args.checkjstring(1);
            if (text.length() > 160) throw new LuaError("notice is limited to 160 characters");
            if (valid()) self.sendNotice(Component.text(text));
            return LuaValue.NIL;
        });
        return context;
    }

    private void bind(LuaTable table, String name, Function<Varargs, LuaValue> function) {
        table.set(name, new VarArgFunction() {
            @Override public Varargs invoke(Varargs args) {
                if (++calls > 32) throw new LuaError("Gameplay API call budget exceeded (32)");
                return function.apply(args);
            }
        });
    }

    ArenaPlayer self() { return self; }
    ScriptArchitect owner() { return owner; }
    boolean valid() { return self.getLifeGeneration() == generation && self.has(owner) && AbilitySupport.active(self); }

    boolean heal(double amount) {
        if (!valid()) return false;
        double before = self.getPlayer().getHealth();
        self.setHealth(before + Math.clamp(amount, 0, 40));
        return self.getPlayer().getHealth() > before;
    }

    boolean dash(double speed, double lift) {
        if (!valid()) return false;
        var direction = self.getPlayer().getEyeLocation().getDirection().setY(0);
        if (direction.lengthSquared() < .001) return false;
        self.getPlayer().setVelocity(direction.normalize().multiply(Math.clamp(speed, 0, 2)).setY(Math.clamp(lift, -.5, 1)));
        return true;
    }

    int cast(String shape, double range, double width, double damage, String kindName, double knockback) {
        if (!hook.equals("on_use") && !hook.equals("on_attack") && !hook.equals("on_tick"))
            throw new LuaError("cast is only available in use, attack or tick hooks; damage reactions cannot recurse");
        calls += 7;
        if (calls > 32) throw new LuaError("Gameplay cast budget exceeded");
        if (!valid()) return 0;
        Combat.DamageKind kind;
        try { kind = Combat.DamageKind.valueOf(kindName); }
        catch (IllegalArgumentException error) { throw new LuaError("Invalid damage kind"); }
        range = Math.clamp(range, .1, 32);
        var eye = self.getPlayer().getEyeLocation();
        switch (shape) {
            case "line", "first" -> CombatEffects.trace(eye, eye.getDirection(), range, Math.clamp(width, 0, 2), Particle.END_ROD);
            case "cone" -> CombatEffects.arc(self.getPlayer(), range, Math.clamp(width, 0, 90));
            case "sphere" -> CombatEffects.ring(self.getPlayer().getLocation().add(0, 1, 0), Math.min(range, 12), Particle.END_ROD, 24);
            default -> throw new LuaError("shape must be first, line, cone or sphere");
        }
        CombatEffects.sound(eye, switch (shape) {
            case "line", "first" -> Sound.ITEM_TRIDENT_THROW;
            case "cone" -> Sound.ENTITY_PLAYER_ATTACK_SWEEP;
            default -> Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        }, .35f, kind == Combat.DamageKind.TOOL || kind == Combat.DamageKind.AREA_TOOL ? 1.4f : .9f);
        List<ArenaPlayer> targets = switch (shape) {
            case "line", "first" -> Targets.line(self, range, Math.clamp(width, 0, 2));
            case "cone" -> Targets.cone(self, range, Math.clamp(width, 0, 90));
            case "sphere" -> Targets.sphere(self, self.getPlayer().getLocation().add(0, 1, 0), Math.min(range, 12));
            default -> throw new LuaError("shape must be first, line, cone or sphere");
        };
        int hits = 0;
        for (ArenaPlayer target : targets.stream().limit(shape.equals("first") ? 1 : 16).toList()) {
            if (!valid()) break;
            if (Combat.damage(self, target, Math.clamp(damage, 0, 40), kind)) {
                hits++;
                Combat.knockback(self, target, Math.clamp(knockback, 0, 2));
            }
        }
        return hits;
    }

    void effect(String preset) {
        if (!valid()) return;
        var at = self.getPlayer().getLocation();
        switch (preset) {
            case "arcane" -> AbilitySupport.pulse(at, Particle.END_ROD, Sound.BLOCK_AMETHYST_BLOCK_CHIME);
            case "heal" -> AbilitySupport.pulse(at, Particle.HAPPY_VILLAGER, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
            case "wind" -> AbilitySupport.pulse(at, Particle.CLOUD, Sound.ENTITY_PLAYER_ATTACK_SWEEP);
            case "strike" -> {
                var eye = self.getPlayer().getEyeLocation();
                double range = Math.clamp(owner.definition.parameter("range", 8), 0, 32);
                double clear = Targets.clearDistance(eye, eye.getDirection(), range);
                AbilitySupport.line(eye, eye.clone().add(eye.getDirection().multiply(clear)), Particle.END_ROD);
            }
            default -> throw new LuaError("effect must be arcane, heal, wind or strike");
        }
    }

    private static PotionEffectType potion(String name) {
        return switch (name) {
            case "speed" -> PotionEffectType.SPEED;
            case "slowness" -> PotionEffectType.SLOWNESS;
            case "resistance" -> PotionEffectType.RESISTANCE;
            case "jump" -> PotionEffectType.JUMP_BOOST;
            case "glowing" -> PotionEffectType.GLOWING;
            default -> throw new LuaError("Unknown buff");
        };
    }

    private static double number(Varargs args, int index, double min, double max) {
        if (args.arg(index).type() != LuaValue.TNUMBER) throw new LuaError("Argument " + index + " must be a number");
        double value = args.checkdouble(index);
        if (!Double.isFinite(value) || value < min || value > max) throw new LuaError("Argument " + index + " out of range");
        return value;
    }

    private ScriptState state() { return ScriptState.of(self, owner.stateKey()); }
}
