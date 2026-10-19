package jp.akkiserver.otogi.api.arena;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.List;

/** State owned by the match, captured before any loadout is applied. */
final class PlayerSnapshot {
    private final ItemStack[] inventory;
    private final GameMode gameMode;
    private final Location location;
    private final List<PotionEffect> effects;
    private final double health, absorption;
    private final int food, fire, freeze, air, level, totalExperience, heldSlot;
    private final float saturation, exhaustion, experience, fallDistance;
    private final boolean allowFlight, flying;
    private final org.bukkit.scoreboard.Scoreboard scoreboard;

    PlayerSnapshot(Player player) {
        player.closeInventory();
        inventory = java.util.Arrays.stream(player.getInventory().getContents())
                .map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
        gameMode = player.getGameMode();
        location = player.getLocation().clone();
        effects = List.copyOf(player.getActivePotionEffects());
        health = player.getHealth();
        absorption = player.getAbsorptionAmount();
        food = player.getFoodLevel();
        saturation = player.getSaturation();
        exhaustion = player.getExhaustion();
        fire = player.getFireTicks();
        freeze = player.getFreezeTicks();
        air = player.getRemainingAir();
        level = player.getLevel();
        totalExperience = player.getTotalExperience();
        experience = player.getExp();
        heldSlot = player.getInventory().getHeldItemSlot();
        fallDistance = player.getFallDistance();
        allowFlight = player.getAllowFlight();
        flying = player.isFlying();
        scoreboard = player.getScoreboard();
    }

    void restore(Player player, double maxHealth) {
        player.closeInventory();
        for (PotionEffect effect : List.copyOf(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
        player.getInventory().setContents(inventory);
        player.getInventory().setHeldItemSlot(heldSlot);
        player.setGameMode(gameMode);
        player.teleport(location);
        player.setVelocity(new org.bukkit.util.Vector());
        player.addPotionEffects(effects);
        var healthAttribute = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        player.setHealth(Math.clamp(health, 0.01D, healthAttribute == null ? maxHealth : healthAttribute.getValue()));
        player.setAbsorptionAmount(absorption);
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setExhaustion(exhaustion);
        player.setFireTicks(fire);
        player.setFreezeTicks(freeze);
        player.setRemainingAir(air);
        player.setLevel(level);
        player.setTotalExperience(totalExperience);
        player.setExp(experience);
        player.setFallDistance(fallDistance);
        player.setAllowFlight(allowFlight);
        player.setFlying(flying);
        player.setScoreboard(scoreboard);
        player.sendActionBar(net.kyori.adventure.text.Component.empty());
    }
}
