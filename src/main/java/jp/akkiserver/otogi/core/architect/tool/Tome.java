package jp.akkiserver.otogi.core.architect.tool;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

/** 具：書 */
public final class Tome extends BaseTool {
    private static final String CIRCLES = "tome.circles";
    private static final class Circles { final Set<Circle> active = new LinkedHashSet<>(); }
    private static double damage() { return CombatSettings.number("tools.tome.damage", 2, 0.05, 40); }
    private static double placeDistance() { return CombatSettings.number("tools.tome.place-distance", 3.5, 0.05, 64); }
    private static double radius() { return CombatSettings.number("tools.tome.radius", 2, 0.05, 40); }
    private static int durationTicks() { return CombatSettings.ticks("tools.tome.duration-ticks", 80); }
    private static int reapplyTicks() { return CombatSettings.ticks("tools.tome.reapply-ticks", 40); }
    private static int slowTicks() { return CombatSettings.ticks("tools.tome.slow-ticks", 30); }

    public Tome() {
        super("tome", "書", Material.ENCHANTED_BOOK, 24, "狙った足元に、魔法陣を敷く",
                "陣に触れた相手へ、鈍足と小ダメージ", "左クリック：設置 / 同時に置ける陣の数には上限がある");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        Player player = self.getPlayer();
        Vector forward = player.getEyeLocation().getDirection().setY(0);
        if (forward.lengthSquared() == 0) {
            forward = new Vector(0, 0, 1);
        }
        forward.normalize();
        double reach = Math.max(0, Targets.clearDistance(player.getEyeLocation(), forward, placeDistance()) - 0.25);
        Location above = player.getLocation().add(forward.clone().multiply(reach)).add(0, 1.5D, 0);
        RayTraceResult ground = player.getWorld().rayTraceBlocks(above, new Vector(0, -1, 0), 5.0D, FluidCollisionMode.NEVER, true);
        if (ground == null) return false;
        Location center = ground.getHitPosition().toLocation(player.getWorld()).add(0, 0.05D, 0);

        CombatEffects.sound(center, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
        Circles circles = self.getState(CIRCLES, Circles.class);
        if (circles == null) { circles = new Circles(); self.setState(CIRCLES, circles); }
        int limit = (int) CombatSettings.number("tools.tome.max-circles", 3, 1, 8);
        if (circles.active.size() >= limit) circles.active.iterator().next().finish();
        Circle circle = new Circle(self, center);
        circles.active.add(circle);
        circle.runTaskTimer(OtogiAPI.plugin.getPlugin(), 0L, 2L);
        return true;
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer self) {
        Circles circles = self.getState(CIRCLES, Circles.class);
        if (circles != null) for (Circle circle : Set.copyOf(circles.active)) circle.finish();
        self.removeState(CIRCLES);
    }

    private final class Circle extends BukkitRunnable {
        private final ArenaPlayer owner;
        private final Location center;
        private final Map<UUID, Integer> readyTicks = new HashMap<>();
        private int age;
        private boolean finished;
        private final int ownerLife;

        private Circle(ArenaPlayer owner, Location center) {
            this.owner = owner;
            this.ownerLife = owner.getLifeGeneration();
            this.center = center;
        }

        @Override
        public void run() {
            if (finished || owner.getLifeGeneration() != ownerLife || age >= durationTicks() || !owner.getArena().isRunning() || !owner.has(Tome.this) || !owner.getPlayer().isOnline()) {
                finish();
                return;
            }
            age += 2;
            draw();

            int now = Bukkit.getCurrentTick();
            for (ArenaPlayer victim : Targets.sphere(owner, center.clone().add(0, 0.8, 0), radius())) {
                Player target = victim.getPlayer();
                if (victim.isInvulnerable() || readyTicks.getOrDefault(target.getUniqueId(), 0) > now) {
                    continue;
                }
                Location at = target.getLocation();
                double dx = at.getX() - center.getX();
                double dz = at.getZ() - center.getZ();
                if (dx * dx + dz * dz > radius() * radius() || at.getY() < center.getY() - 1.0D || at.getY() > center.getY() + 2.0D) {
                    continue;
                }
                readyTicks.put(target.getUniqueId(), now + reapplyTicks());
                boolean landed = Combat.damage(owner, victim, damage(), Combat.DamageKind.AREA_TOOL);
                if (finished || owner.getLifeGeneration() != ownerLife || !owner.getArena().isRunning()) return;
                if (!landed) continue;
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks(), 1, false, true));
                CombatEffects.sound(at, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8F, 1.4F);
            }
        }

        private void draw() {
            if (!CombatSettings.enabled("feedback.particles")) return;
            double spin = age * 0.12D;
            for (int i = 0; i < 24; i++) {
                double angle = spin + i * Math.PI * 2 / 24;
                center.getWorld().spawnParticle(Particle.ENCHANT, center.clone().add(Math.cos(angle) * radius(), 0.1D, Math.sin(angle) * radius()), 1, 0, 0, 0, 0);
            }
            // inner star
            for (int i = 0; i < 3; i++) {
                double angle = -spin + i * Math.PI * 2 / 3;
                double next = angle + Math.PI * 2 / 3 * 1.0D;
                for (double t = 0; t < 1; t += 0.25D) {
                    double x = Math.cos(angle) * (1 - t) + Math.cos(next) * t;
                    double z = Math.sin(angle) * (1 - t) + Math.sin(next) * t;
                    center.getWorld().spawnParticle(Particle.END_ROD, center.clone().add(x * radius() * 0.8D, 0.1D, z * radius() * 0.8D), 1, 0, 0, 0, 0);
                }
            }
        }

        private void finish() {
            if (finished) return;
            finished = true;
            cancel();
            Circles circles = owner.getState(CIRCLES, Circles.class);
            if (circles != null) circles.active.remove(this);
            CombatEffects.ring(center, radius(), Particle.ENCHANT, 16);
        }
    }
}
