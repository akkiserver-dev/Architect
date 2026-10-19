package jp.akkiserver.otogi.core.listener;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.util.Combat;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.ArchitectItems;

import java.util.ArrayList;

/**
 * Bridges Bukkit events to the running arena and its architects.
 */
public final class ArenaListener implements Listener {
    @Nullable
    private static ArenaPlayer running(@NotNull org.bukkit.entity.Entity entity) {
        if (!(entity instanceof Player player)) {
            return null;
        }
        ArenaPlayer arenaPlayer = OtogiAPI.getArenaPlayer(player);
        return arenaPlayer != null && arenaPlayer.getArena().isRunning() ? arenaPlayer : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        ArenaPlayer victim = running(event.getEntity());
        if (victim == null) {
            if (event instanceof EntityDamageByEntityEvent attack) {
                org.bukkit.entity.Entity source = attack.getDamager();
                if (source instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) source = shooter;
                if (running(source) != null) event.setCancelled(true);
            }
            return;
        }
        int victimLife = victim.getLifeGeneration();
        if (victim.isEliminated()) { event.setCancelled(true); return; }

        // vanilla melee is replaced by the custom attack of the tool
        if (event instanceof EntityDamageByEntityEvent melee && melee.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && melee.getDamager() instanceof Player damager && !Combat.isDealing()) {
            ArenaPlayer owner = running(damager);
            if (owner != null) {
                event.setCancelled(true);
                attack(owner);
                return;
            }
        }

        if (victim.isInvulnerable()) {
            event.setCancelled(true);
            return;
        }
        if (!Combat.isDealing() && event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            event.setCancelled(true);
            return;
        }

        ArenaPlayer attacker = null;
        boolean self = false;
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            org.bukkit.entity.Entity damager = byEntity.getDamager();
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
                damager = shooter;
            }
            if (damager instanceof Player player) {
                if (player.getUniqueId().equals(victim.getPlayer().getUniqueId())) {
                    self = true;
                } else {
                    attacker = victim.getArena().getPlayer(player);
                    if (attacker == null) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (!self) {
            if (attacker != null && !Combat.isSecondaryDamage()) {
                for (Architect architect : new ArrayList<>(attacker.getArchitects())) {
                    if (event instanceof EntityDamageByEntityEvent byEntity) {
                        architect.onDamageDealt(attacker, victim, byEntity);
                    }
                }
            }
            for (Architect architect : new ArrayList<>(victim.getArchitects())) {
                architect.onDamageTaken(victim, attacker, event);
            }
            if (event.isCancelled()) {
                return;
            }
        }

        if (!self && !Combat.isSecondaryDamage() && event.getFinalDamage() > 0 && victim.getArena().isRunning() && victim.getLifeGeneration() == victimLife) {
            for (Architect architect : new ArrayList<>(victim.getArchitects())) {
                architect.onDamageTakenConfirmed(victim, attacker, event);
            }
        }

        if (attacker != null && victim.getArena().isRunning() && victim.getLifeGeneration() == victimLife) {
            victim.setLastAttacker(attacker);
            if (!Combat.isSecondaryDamage() && event instanceof EntityDamageByEntityEvent byEntity && event.getFinalDamage() > 0) {
                for (Architect architect : new ArrayList<>(attacker.getArchitects())) {
                    architect.onDamageConfirmed(attacker, victim, byEntity);
                }
            }
        }

        if (!victim.getArena().isRunning() || victim.getLifeGeneration() != victimLife) {
            event.setCancelled(true);
            return;
        }

        double remaining = victim.getPlayer().getHealth() - event.getFinalDamage();
        if (remaining > 0.0D) {
            return;
        }

        for (Architect architect : new ArrayList<>(victim.getArchitects())) {
            if (architect.onFatalDamage(victim, event)) {
                event.setCancelled(true);
                return;
            }
        }

        event.setCancelled(true);
        victim.getArena().handleDeath(victim, attacker != null ? attacker : victim.getRecentAttacker());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        ArenaPlayer player = running(event.getEntity());
        if (player == null) {
            return;
        }
        if (event.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN
                && event.getRegainReason() != EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }
        for (Architect architect : player.getArchitects()) {
            if (!architect.canRegenerate(player)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ArenaPlayer player = running(event.getPlayer());
        if (player == null) {
            return;
        }
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            attack(player);
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (ArchitectItems.read(event.getItem(), player) instanceof Skill skill) {
            event.setCancelled(true);
            player.getArena().useSkill(player, skill);
        } else if (ArchitectItems.read(event.getItem(), player) instanceof Tool tool && player.has(tool)) {
            event.setCancelled(true);
            if (tool.onUse(player)) player.setInvulnerable(0);
        }
    }

    /**
     * Attacks with the tool in hand. Skill items never attack.
     */
    private static void attack(@NotNull ArenaPlayer player) {
        if (ArchitectItems.read(player.getPlayer().getInventory().getItemInMainHand(), player) instanceof Tool tool) {
            player.getArena().attack(player, tool);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        if (running(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (running(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (running(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (running(event.getWhoClicked()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (running(event.getWhoClicked()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (running(event.getEntity()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        ArenaPlayer player = running(event.getPlayer());
        if (player == null) return;
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;
        Architect held = ArchitectItems.read(event.getPlayer().getInventory().getItemInMainHand(), player);
        if (held instanceof Skill skill) player.getArena().useSkill(player, skill);
        else if (held instanceof Tool tool && player.has(tool) && tool.onUse(player)) player.setInvulnerable(0);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (running(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (running(event.getPlayer()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ArenaPlayer player = OtogiAPI.getArenaPlayer(event.getPlayer());
        if (player != null) {
            player.getArena().removePlayer(event.getPlayer());
        }
    }
}
