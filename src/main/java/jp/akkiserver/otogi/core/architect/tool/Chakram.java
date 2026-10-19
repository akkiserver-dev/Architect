package jp.akkiserver.otogi.core.architect.tool;

import jp.akkiserver.otogi.core.architect.passive.Foresight;
import org.bukkit.Location;
import jp.akkiserver.otogi.core.combat.CombatSettings;
import jp.akkiserver.otogi.core.combat.CombatEffects;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.util.Targets;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 具：輪刃 */
public final class Chakram extends BaseTool {
    private static final String FLIGHT = "chakram.flight";
    private static double damage() { return CombatSettings.number("tools.chakram.damage", 5, 0.05, 40); }
    private static double returnRate() { return CombatSettings.number("tools.chakram.return-rate", 0.6, 0.05, 40); }
    private static double range() { return CombatSettings.number("tools.chakram.range", 12, 0.05, 64); }
    private static double outSpeed() { return CombatSettings.number("tools.chakram.out-speed", 1.1, 0.05, 40); }
    private static double returnSpeed() { return CombatSettings.number("tools.chakram.return-speed", 1.4, 0.05, 40); }
    private static double hitRadius() { return CombatSettings.number("tools.chakram.hit-radius", 0.9, 0.05, 40); }
    private static int maxReturnTicks() { return CombatSettings.ticks("tools.chakram.max-return-ticks", 80); }
    private static int catchCoolTime() { return CombatSettings.ticks("tools.chakram.catch-cool-time", 8); }

    public Chakram() {
        super("chakram", "輪刃", Material.GOLD_INGOT, 10, "通常攻撃で投擲武器を放ち、命中後に手元へ戻る",
                "行きと帰りに一度ずつ命中。戻り刃の威力は低め", "投擲中は通常攻撃できない");
    }

    @Override
    public boolean onAttack(@NotNull ArenaPlayer self) {
        if (self.hasState(FLIGHT)) {
            return false;
        }
        Player player = self.getPlayer();
        Location eye = player.getEyeLocation();
        Location start = eye.clone().subtract(0, 0.3D, 0).add(eye.getDirection().multiply(0.6D));
        Flight flight = new Flight(self, start, eye.getDirection());
        self.setState(FLIGHT, flight);
        for (ArenaPlayer target : self.getArena().getPlayers()) {
            if (target != self && target.getPlayer().getWorld() == player.getWorld())
                Foresight.warn(self, target, start, eye.getDirection());
        }
        flight.runTaskTimer(OtogiAPI.plugin.getPlugin(), 0L, 1L);
        CombatEffects.sound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0F, 1.6F);
        return true;
    }

    @Override
    public void onUnequip(@NotNull ArenaPlayer player) {
        Flight flight = player.getState(FLIGHT, Flight.class);
        if (flight != null) {
            flight.finish(false);
        }
    }

    private final class Flight extends BukkitRunnable {
        private final ArenaPlayer owner;
        private final Location position;
        private final Vector direction;
        private final Set<UUID> hit = new HashSet<>();
        private final ItemDisplay display;
        private double travelled;
        private boolean returning;
        private int returnAge;
        private float spin;
        private boolean finished;
        private final int ownerLife;

        private Flight(ArenaPlayer owner, Location start, Vector direction) {
            this.owner = owner;
            this.ownerLife = owner.getLifeGeneration();
            this.position = start.clone();
            this.direction = direction.clone().normalize();
            this.display = start.getWorld().spawn(start, ItemDisplay.class, entity -> {
                entity.setItemStack(new ItemStack(Material.GOLD_INGOT));
                entity.setTeleportDuration(1);
                entity.setInterpolationDuration(1);
                entity.setPersistent(false);
            });
        }

        @Override
        public void run() {
            if (finished || owner.getLifeGeneration() != ownerLife || !owner.getArena().isRunning() || !owner.has(Chakram.this) || !owner.getPlayer().isOnline()
                    || owner.getPlayer().getWorld() != position.getWorld()) {
                finish(false);
                return;
            }

            Location previous = position.clone();
            boolean wasReturning = returning;
            if (!returning) {
                double step = Math.max(0, Math.min(outSpeed(), range() - travelled));
                double clear = Targets.clearDistance(position, direction, step + 0.3D);
                if (clear < step + 0.3D) {
                    turn();
                } else {
                    position.add(direction.clone().multiply(step));
                    travelled += step;
                }
            } else {
                Location target = owner.getPlayer().getLocation().add(0, 1.0D, 0);
                Vector toOwner = target.toVector().subtract(position.toVector());
                if (++returnAge > maxReturnTicks() || toOwner.length() <= returnSpeed()) {
                    finish(true);
                    return;
                }
                position.add(toOwner.normalize().multiply(returnSpeed()));
            }

            Vector movement = position.toVector().subtract(previous.toVector());
            var victims = movement.lengthSquared() < 0.00001D ? Targets.sphere(owner, position, hitRadius())
                    : Targets.line(owner, previous, movement, movement.length(), hitRadius());
            for (ArenaPlayer victim : victims) {
                if (hit.add(victim.getPlayer().getUniqueId())) {
                    Combat.damage(owner, victim, wasReturning ? damage() * returnRate() : damage());
                    if (finished || owner.getLifeGeneration() != ownerLife || !owner.getArena().isRunning()) return;
                    CombatEffects.sound(position, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.3F);
                }
            }
            if (!returning && (!victims.isEmpty() || travelled >= range())) turn();
            render();
        }

        /** Starts the return. Enemies hit on the way out can be hit again. */
        private void turn() {
            returning = true;
            hit.clear();
            CombatEffects.sound(position, Sound.BLOCK_CHAIN_HIT, 1.0F, 1.5F);
        }

        private void render() {
            spin += 0.9F;
            display.teleport(position);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(new Vector3f(), new Quaternionf().rotateY(spin).rotateX((float) (Math.PI / 2)),
                    new Vector3f(1.2F, 1.2F, 1.2F), new Quaternionf()));
            if (CombatSettings.enabled("feedback.particles")) position.getWorld().spawnParticle(Particle.CRIT, position, 2, 0.1, 0.1, 0.1, 0);
        }

        private void finish(boolean caught) {
            if (finished) return;
            finished = true;
            cancel();
            display.remove();
            owner.removeState(FLIGHT);
            if (caught) {
                owner.setAttackCoolTime(catchCoolTime());
                CombatEffects.sound(owner.getPlayer().getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 1.0F, 1.5F);
            }
        }
    }
}
