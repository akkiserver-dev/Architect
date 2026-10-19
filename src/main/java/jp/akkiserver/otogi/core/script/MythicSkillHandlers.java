package jp.akkiserver.otogi.core.script;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.architect.AbilitySupport;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;

import java.util.Map;
import java.util.Set;

/** Bounded Java mechanics; JSON owns the numbers, ArenaPlayer owns every mutable effect. */
final class MythicSkillHandlers {
    private static final Set<String> HANDLERS = Set.of("thunder", "frost_cone", "oni_frenzy", "dread_sphere");
    private record Bound(double min, double max, boolean integral) {}
    private static Bound value(double min, double max) { return new Bound(min, max, false); }
    private static Bound ticks(int min, int max) { return new Bound(min, max, true); }
    private static final Map<String, Map<String, Bound>> BOUNDS = Map.of(
            "thunder", Map.of("range", value(.1, 32), "width", value(0, 2), "delayTicks", ticks(4, 100),
                    "damage", value(.1, 40), "knockback", value(0, 2)),
            "frost_cone", Map.of("range", value(.1, 16), "halfAngle", value(1, 75), "damage", value(.1, 40),
                    "slowTicks", ticks(1, 200), "slowAmplifier", ticks(0, 2)),
            "oni_frenzy", Map.of("durationTicks", ticks(1, 200), "toolMultiplier", value(1, 2), "incomingMultiplier", value(1, 2)));

    static boolean supports(String handler) { return HANDLERS.contains(handler); }

    static void validate(String handler, Map<String, Double> parameters) {
        if (handler.equals("dread_sphere")) { DreadSphereHandler.validate(parameters); return; }
        Map<String, Bound> bounds = BOUNDS.get(handler);
        if (bounds == null) return;
        parameters.forEach((key, number) -> {
            Bound bound = bounds.get(key);
            if (bound == null) throw new IllegalArgumentException("Unknown " + handler + " parameter: " + key);
            if (!Double.isFinite(number) || number < bound.min || number > bound.max || (bound.integral && number != Math.rint(number)))
                throw new IllegalArgumentException(handler + " parameter out of range: " + key);
        });
    }

    static boolean handle(GameplayContext context, String hook, EntityDamageEvent event) {
        if (!context.valid() || !context.self().has(context.owner())) return false;
        return switch (context.owner().definition.handler()) {
            case "thunder" -> {
                if (hook.equals("on_tick")) tickThunder(context);
                yield hook.equals("on_use") && thunder(context);
            }
            case "frost_cone" -> hook.equals("on_use") && frost(context);
            case "oni_frenzy" -> frenzy(context, hook, event);
            case "dread_sphere" -> DreadSphereHandler.handle(context, hook);
            default -> false;
        };
    }

    private static String effectKey(ScriptArchitect owner) { return owner.stateKey() + ".mythic"; }

    static void clear(ScriptArchitect owner, ArenaPlayer self) {
        DreadSphereHandler.clear(owner, self);
        PendingStrike pending = self.getState(effectKey(owner), PendingStrike.class);
        if (pending != null) cancelStrike(owner, self, pending);
        else self.removeState(effectKey(owner));
    }

    // Cancel the captured task even when a life reset has already removed its state.
    // An obsolete task must never clear a replacement strike belonging to the next life.
    static void cancelStrike(ScriptArchitect owner, ArenaPlayer self, PendingStrike pending) {
        if (pending.task != null) pending.task.cancel();
        if (self.getState(effectKey(owner), PendingStrike.class) == pending) self.removeState(effectKey(owner));
    }

    static final class PendingStrike {
        final Location origin;
        final Vector direction;
        final double reach;
        final int life;
        final int due;
        int nextVisual;
        BukkitTask task;
        PendingStrike(Location origin, double reach, int life, int due) {
            this.origin = origin.clone();
            this.direction = origin.getDirection().normalize();
            this.reach = reach;
            this.life = life;
            this.due = due;
        }
    }

    private static boolean thunder(GameplayContext context) {
        ArenaPlayer self = context.self();
        ScriptArchitect owner = context.owner();
        if (self.hasState(effectKey(owner))) return false;
        ArchitectDefinition definition = owner.definition;
        Location eye = self.getPlayer().getEyeLocation();
        double reach = Targets.clearDistance(eye, eye.getDirection(), definition.parameter("range", 12));
        if (reach < .1) return false;
        PendingStrike pending = new PendingStrike(eye, reach, self.getLifeGeneration(),
                Bukkit.getCurrentTick() + (int) definition.parameter("delayTicks", 20));
        self.setState(effectKey(owner), pending);
        showThunder(pending, definition.parameter("width", .45), Particle.ELECTRIC_SPARK);
        pending.nextVisual = Bukkit.getCurrentTick() + 4;
        CombatEffects.sound(eye, Sound.BLOCK_NOTE_BLOCK_BELL, .6f, 1.8f);
        // The stored task is canceled on unequip/failure; every callback also checks life and match.
        pending.task = Bukkit.getScheduler().runTaskTimer(OtogiAPI.plugin.getPlugin(), () -> {
            if (self.getLifeGeneration() != pending.life || !AbilitySupport.active(self) || !self.has(owner)
                    || owner.hasFailed(self) || self.getPlayer().getWorld() != pending.origin.getWorld()
                    || self.getState(effectKey(owner), PendingStrike.class) != pending) {
                cancelStrike(owner, self, pending);
                return;
            }
            owner.invoke("on_tick", self, null);
        }, 1, 1);
        return true;
    }

    private static void tickThunder(GameplayContext context) {
        ScriptArchitect owner = context.owner();
        ArenaPlayer self = context.self();
        PendingStrike pending = self.getState(effectKey(owner), PendingStrike.class);
        if (pending == null) return;
        if (self.getLifeGeneration() != pending.life || self.getPlayer().getWorld() != pending.origin.getWorld()) {
            clear(owner, self);
            return;
        }
        int now = Bukkit.getCurrentTick();
        double width = owner.definition.parameter("width", .45);
        if (now < pending.due) {
            if (now >= pending.nextVisual) {
                showThunder(pending, width, Particle.ELECTRIC_SPARK);
                pending.nextVisual = now + 4;
            }
            return;
        }
        clear(owner, self);
        showThunder(pending, width, Particle.END_ROD);
        CombatEffects.sound(pending.origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, .65f, 1.7f);
        for (ArenaPlayer target : Targets.line(self, pending.origin, pending.direction, pending.reach, width).stream().limit(16).toList()) {
            if (!context.valid() || self.getLifeGeneration() != pending.life || !self.has(owner)) break;
            if (Combat.damage(self, target, owner.definition.parameter("damage", 7), Combat.DamageKind.AREA_SKILL))
                Combat.knockback(self, target, owner.definition.parameter("knockback", .35));
        }
    }

    private static void showThunder(PendingStrike pending, double width, Particle particle) {
        CombatEffects.trace(pending.origin, pending.direction, pending.reach, width, particle);
    }

    private static boolean frost(GameplayContext context) {
        ArenaPlayer self = context.self();
        ArchitectDefinition definition = context.owner().definition;
        double range = definition.parameter("range", 6);
        double angle = definition.parameter("halfAngle", 32);
        frostOutline(self.getPlayer().getEyeLocation(), range, angle);
        CombatEffects.sound(self.getPlayer().getLocation(), Sound.ENTITY_PLAYER_BREATH, .7f, .7f);
        for (ArenaPlayer target : Targets.cone(self, range, angle).stream().limit(16).toList()) {
            if (!context.valid() || !self.has(context.owner())) break;
            if (Combat.damage(self, target, definition.parameter("damage", 3), Combat.DamageKind.AREA_SKILL))
                target.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                        (int) definition.parameter("slowTicks", 60), (int) definition.parameter("slowAmplifier", 0), false, true, true));
        }
        return true;
    }

    private static void frostOutline(Location eye, double range, double halfAngle) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        Vector forward = eye.getDirection().normalize();
        Vector right = new Vector(Math.cos(Math.toRadians(eye.getYaw())), 0, Math.sin(Math.toRadians(eye.getYaw())));
        for (int i = 0; i <= 16; i++) {
            double angle = Math.toRadians(-halfAngle + 2 * halfAngle * i / 16);
            Vector direction = forward.clone().multiply(Math.cos(angle)).add(right.clone().multiply(Math.sin(angle))).normalize();
            double visible = Targets.clearDistance(eye, direction, range);
            Location end = eye.clone().add(direction.clone().multiply(visible));
            eye.getWorld().spawnParticle(Particle.SNOWFLAKE, end, 1, 0, 0, 0, 0);
            if (i == 0 || i == 8 || i == 16) AbilitySupport.line(eye, end, Particle.SNOWFLAKE);
        }
    }

    private record Frenzy(int life, int until) {}

    private static boolean frenzy(GameplayContext context, String hook, EntityDamageEvent event) {
        ArenaPlayer self = context.self();
        ScriptArchitect owner = context.owner();
        ArchitectDefinition definition = owner.definition;
        int now = Bukkit.getCurrentTick();
        Frenzy frenzy = self.getState(effectKey(owner), Frenzy.class);
        if (frenzy != null && (frenzy.life != self.getLifeGeneration() || now >= frenzy.until)) {
            clear(owner, self);
            if (hook.equals("on_tick")) CombatEffects.personalSound(self.getPlayer(), Sound.BLOCK_BEACON_DEACTIVATE, .4f, 1.4f);
            frenzy = null;
        }
        if (hook.equals("on_use")) {
            if (frenzy != null) return false;
            self.setState(effectKey(owner), new Frenzy(self.getLifeGeneration(), now + (int) definition.parameter("durationTicks", 80)));
            self.sendNotice(Component.text("鬼宴: 具威力↑ / 被ダメージ↑", NamedTextColor.RED));
            AbilitySupport.ring(self.getPlayer().getLocation().add(0, .8, 0), .7, Particle.CRIMSON_SPORE);
            CombatEffects.sound(self.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, .7f, .6f);
            return true;
        }
        if (frenzy == null) return false;
        if (event != null && hook.equals("on_damage_dealt") && Combat.isDealing() && Combat.getDamageKind() == Combat.DamageKind.TOOL)
            event.setDamage(Math.clamp(event.getDamage() * definition.parameter("toolMultiplier", 1.3), 0, 100));
        if (event != null && hook.equals("on_damage_taken"))
            event.setDamage(Math.clamp(event.getDamage() * definition.parameter("incomingMultiplier", 1.25), 0, 100));
        if (hook.equals("on_tick")) AbilitySupport.ring(self.getPlayer().getLocation().add(0, .8, 0), .7, Particle.CRIMSON_SPORE);
        return false;
    }

    private MythicSkillHandlers() {}
}
