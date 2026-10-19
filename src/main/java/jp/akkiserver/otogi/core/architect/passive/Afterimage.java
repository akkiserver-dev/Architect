package jp.akkiserver.otogi.core.architect.passive;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tickable;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.core.architect.BaseArchitect;
import jp.akkiserver.otogi.core.architect.AbilitySupport;
import java.util.HashMap;
import java.util.UUID;

/** Ephemeral, damageable decoys. No persistent entities or repeating global scans. */
public final class Afterimage extends BaseArchitect implements Passive, Tickable, Listener {
    private static final String LAST = "afterimage.last", ENTITY = "afterimage.entity";
    private final java.util.Map<UUID, Decoy> decoys = new HashMap<>();
    private static final class Decoy {
        final ArenaPlayer owner; final ArmorStand entity; double health = 4;
        Decoy(ArenaPlayer owner, ArmorStand entity) { this.owner = owner; this.entity = entity; }
    }
    public Afterimage() { super("afterimage", "残影", "素早く移動した先に、2秒間の残像を残す", "残像の体力4 / 壊した相手に2秒間の盲目 / CT 10秒"); }
    @Override public void onEquip(ArenaPlayer self) { self.setState(LAST, self.getPlayer().getLocation()); }
    @Override public void onTick(ArenaPlayer self) {
        Location previous = self.getState(LAST, Location.class);
        Location now = self.getPlayer().getLocation();
        if (previous != null && previous.getWorld() == now.getWorld() && previous.distanceSquared(now) >= 16) spawn(self);
        self.setState(LAST, now);
    }
    @Override public void onSkillUse(ArenaPlayer self, Skill skill) {
        if (skill.getId().getKey().equals("warp")) spawn(self);
    }
    private void spawn(ArenaPlayer self) {
        if (self.getRemainingCoolTime(this) > 0) return;
        self.setCoolTime(this, AbilitySupport.ticks(this, "cooldown", 10));
        remove(self);
        var stand = self.getPlayer().getWorld().spawn(self.getPlayer().getLocation(), ArmorStand.class, entity -> {
            entity.setPersistent(false); entity.setGravity(false); entity.setInvisible(true);
            entity.setArms(true); entity.setBasePlate(false); entity.setSilent(true);
            entity.customName(self.getPlayer().displayName());
            var head = new ItemStack(Material.PLAYER_HEAD);
            var meta = (SkullMeta) head.getItemMeta(); meta.setOwningPlayer(self.getPlayer()); head.setItemMeta(meta);
            equipDecoy(entity, head, new ItemStack(Material.LEATHER_CHESTPLATE), new ItemStack(Material.LEATHER_LEGGINGS));
        });
        self.setState(ENTITY, stand.getUniqueId());
        decoys.put(stand.getUniqueId(), new Decoy(self, stand));
        AbilitySupport.pulse(stand.getLocation(), Particle.CLOUD, Sound.ENTITY_ILLUSIONER_MIRROR_MOVE);
        org.bukkit.Bukkit.getScheduler().runTaskLater(OtogiAPI.plugin.getPlugin(), () -> {
            decoys.remove(stand.getUniqueId()); stand.remove();
        }, AbilitySupport.ticks(this, "duration", 2));
    }
    /** Armor stands are not Mobs: their equipment cannot use drop-chance setters. */
    static void equipDecoy(ArmorStand entity, ItemStack head, ItemStack chest, ItemStack legs) {
        entity.getEquipment().setHelmet(head);
        entity.getEquipment().setChestplate(chest);
        entity.getEquipment().setLeggings(legs);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            entity.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
            entity.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
        }
    }
    @EventHandler public void onManipulate(PlayerArmorStandManipulateEvent event) {
        if (decoys.containsKey(event.getRightClicked().getUniqueId())) event.setCancelled(true);
    }
    @EventHandler public void onEnvironment(EntityDamageEvent event) {
        if (!(event instanceof EntityDamageByEntityEvent) && decoys.containsKey(event.getEntity().getUniqueId())) event.setCancelled(true);
    }
    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Decoy decoy = decoys.get(event.getEntity().getUniqueId());
        if (decoy == null) return;
        event.setCancelled(true);
        if (!(event.getDamager() instanceof Player player)) return;
        var attacker = OtogiAPI.getArenaPlayer(player);
        if (attacker == null || attacker == decoy.owner || attacker.getArena() != decoy.owner.getArena()) return;
        decoy.health -= event.getFinalDamage();
        AbilitySupport.pulse(decoy.entity.getLocation(), Particle.CLOUD, Sound.BLOCK_GLASS_HIT);
        if (decoy.health <= 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, true));
            remove(decoy.owner);
        }
    }
    private void remove(ArenaPlayer self) {
        UUID id = self.getState(ENTITY, UUID.class);
        Decoy decoy = id == null ? null : decoys.remove(id);
        if (decoy != null) decoy.entity.remove();
        self.removeState(ENTITY);
    }
    @Override public void onUnequip(ArenaPlayer self) { remove(self); self.removeState(LAST); }
}
