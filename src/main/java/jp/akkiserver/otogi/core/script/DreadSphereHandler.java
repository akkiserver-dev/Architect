package jp.akkiserver.otogi.core.script;

import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

/** Fixed, telegraphed sphere. No entities, blocks or player references enter Lua. */
final class DreadSphereHandler {
    private static String key(ScriptArchitect owner) { return owner.stateKey() + ".dread"; }

    static void validate(Map<String, Double> parameters) {
        parameters.forEach((name, value) -> {
            boolean allowed = Double.isFinite(value) && switch (name) {
                case "range" -> value >= .5 && value <= 24;
                case "radius" -> value >= .5 && value <= 6;
                case "damage" -> value >= .1 && value <= 20;
                case "delayTicks" -> value == Math.rint(value) && value >= 8 && value <= 100;
                case "silenceTicks" -> value == Math.rint(value) && value >= 0 && value <= 100;
                default -> false;
            };
            if (!allowed) throw new IllegalArgumentException("Invalid dread_sphere parameter: " + name);
        });
    }

    static final class Pending {
        final Location center;
        final int life, due;
        int nextVisual;
        BukkitTask task;
        Pending(Location center, int life, int due) {
            this.center = center.clone();
            this.life = life;
            this.due = due;
        }
    }

    static void cancel(ScriptArchitect owner, ArenaPlayer self, Pending pending) {
        if (pending.task != null) pending.task.cancel();
        if (self.getState(key(owner), Pending.class) == pending) self.removeState(key(owner));
    }

    static void clear(ScriptArchitect owner, ArenaPlayer self) {
        Pending pending = self.getState(key(owner), Pending.class);
        if (pending != null) cancel(owner, self, pending);
    }

    static boolean handle(GameplayContext context, String hook) {
        if (hook.equals("on_use")) return start(context);
        if (hook.equals("on_tick")) tick(context);
        return false;
    }

    private static boolean start(GameplayContext context) {
        var self = context.self();
        var owner = context.owner();
        if (self.hasState(key(owner))) return false;
        var definition = owner.definition;
        var eye = self.getPlayer().getEyeLocation();
        double clear = Targets.clearDistance(eye, eye.getDirection(), definition.parameter("range", 10));
        if (clear < .2) return false;
        Location center = eye.clone().add(eye.getDirection().multiply(Math.max(0, clear - .15)));
        int now = Bukkit.getCurrentTick();
        Pending pending = new Pending(center, self.getLifeGeneration(), now + (int) definition.parameter("delayTicks", 24));
        self.setState(key(owner), pending);
        outline(center, definition.parameter("radius", 2.5), Particle.SOUL_FIRE_FLAME);
        pending.nextVisual = now + 4;
        CombatEffects.sound(center, Sound.BLOCK_BELL_RESONATE, .5f, .6f);
        self.sendNotice(Component.text("呼び声: 予告領域を置いた"));
        pending.task = Bukkit.getScheduler().runTaskTimer(OtogiAPI.plugin.getPlugin(), () -> {
            if (!context.valid() || owner.hasFailed(self) || self.getLifeGeneration() != pending.life
                    || self.getPlayer().getWorld() != pending.center.getWorld()
                    || self.getState(key(owner), Pending.class) != pending) {
                cancel(owner, self, pending);
                return;
            }
            owner.invoke("on_tick", self, null);
        }, 1, 1);
        return true;
    }

    private static void tick(GameplayContext context) {
        var self = context.self();
        var owner = context.owner();
        Pending pending = self.getState(key(owner), Pending.class);
        if (pending == null) return;
        if (self.getLifeGeneration() != pending.life || self.getPlayer().getWorld() != pending.center.getWorld()) {
            cancel(owner, self, pending);
            return;
        }
        int now = Bukkit.getCurrentTick();
        double radius = owner.definition.parameter("radius", 2.5);
        if (now < pending.due) {
            if (now >= pending.nextVisual) {
                outline(pending.center, radius, Particle.SOUL_FIRE_FLAME);
                pending.nextVisual = now + 4;
            }
            return;
        }
        cancel(owner, self, pending);
        outline(pending.center, radius, Particle.SOUL);
        CombatEffects.sound(pending.center, Sound.ENTITY_WARDEN_SONIC_BOOM, .45f, 1.5f);
        for (var target : Targets.sphere(self, pending.center, radius).stream().limit(16).toList()) {
            if (!context.valid()) break;
            if (!Combat.damage(self, target, owner.definition.parameter("damage", 4), Combat.DamageKind.AREA_SKILL)) continue;
            int duration = (int) owner.definition.parameter("silenceTicks", 30);
            if (duration <= 0 || !context.valid()) continue;
            String silence = "effects.silenced.until";
            Integer previous = target.getState(silence, Integer.class);
            target.setState(silence, silenceDeadline(previous, now, duration));
            target.sendNotice(Component.text("呼び声: 術を封じられた"));
        }
    }

    static int silenceDeadline(Integer previous, int now, int duration) {
        return Math.max(previous == null ? now : previous, now + duration);
    }

    private static void outline(Location center, double radius, Particle particle) {
        if (!CombatSettings.enabled("feedback.particles")) return;
        // Three great circles show vertical reach as well as the footprint.
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12;
            double a = Math.cos(angle) * radius, b = Math.sin(angle) * radius;
            center.getWorld().spawnParticle(particle, center.clone().add(a, 0, b), 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(particle, center.clone().add(a, b, 0), 1, 0, 0, 0, 0);
            center.getWorld().spawnParticle(particle, center.clone().add(0, a, b), 1, 0, 0, 0, 0);
        }
    }

    private DreadSphereHandler() {}
}
