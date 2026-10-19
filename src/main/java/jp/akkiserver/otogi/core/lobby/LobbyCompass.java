package jp.akkiserver.otogi.core.lobby;

import jp.akkiserver.otogi.api.OtogiAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/** A tagged lobby shortcut; occupied slots and ordinary compasses are left alone. */
public final class LobbyCompass implements Listener {
    private static final NamespacedKey KEY = new NamespacedKey("otogi", "lobby_menu");
    private final LobbyMenu menu;
    private final BukkitTask refresh;

    public LobbyCompass(JavaPlugin plugin, LobbyMenu menu) {
        this.menu = menu;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Also covers joining, respawning and returning from a match after inventory restoration.
        refresh = Bukkit.getScheduler().runTaskTimer(plugin,
                () -> Bukkit.getOnlinePlayers().forEach(this::supply), 1L, 20L);
    }

    static boolean inLobby(Player player) {
        var participant = OtogiAPI.getArenaPlayer(player);
        return participant == null || !participant.getArena().isRunning();
    }

    private boolean isCompass(ItemStack item) {
        return item != null && item.getType() == Material.COMPASS
                && item.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    private void supply(Player player) {
        if (!inLobby(player) || player.isDead()) return;
        var inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) if (isCompass(item)) return;
        if (isCompass(player.getItemOnCursor())) return;
        int slot = inventory.firstEmpty();
        if (slot < 0) return;
        ItemStack compass = new ItemStack(Material.COMPASS);
        compass.editMeta(meta -> {
            meta.itemName(Component.text("御伽のメニュー", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("右クリックでロビーを開く", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        });
        inventory.setItem(slot, compass);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()
                || !inLobby(event.getPlayer()) || !isCompass(event.getItem())) return;
        event.setCancelled(true);
        menu.open(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isCompass(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        // Keep the shortcut out of containers, including number-key and offhand swaps.
        if (event.getClickedInventory() == null) {
            if (isCompass(event.getCursor())) event.setCancelled(true);
            return;
        }
        int hotbar = event.getHotbarButton();
        ItemStack swapped = hotbar >= 0 ? event.getWhoClicked().getInventory().getItem(hotbar)
                : event.getClick() == org.bukkit.event.inventory.ClickType.SWAP_OFFHAND
                ? event.getWhoClicked().getInventory().getItemInOffHand() : null;
        boolean involvesCompass = isCompass(event.getCurrentItem()) || isCompass(event.getCursor()) || isCompass(swapped);
        boolean containerSlot = event.getRawSlot() < event.getView().getTopInventory().getSize();
        if (involvesCompass && (containerSlot || event.isShiftClick() || event.getClick().isCreativeAction()
                || event.getClick() == org.bukkit.event.inventory.ClickType.DROP
                || event.getClick() == org.bukkit.event.inventory.ClickType.CONTROL_DROP)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (isCompass(event.getOldCursor()) && event.getRawSlots().stream()
                .anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) event.setCancelled(true);
    }

    @EventHandler public void onDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isCompass);
    }

    public void close() {
        refresh.cancel();
        for (Player player : Bukkit.getOnlinePlayers()) {
            var inventory = player.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++)
                if (isCompass(inventory.getItem(slot))) inventory.setItem(slot, null);
            if (isCompass(player.getItemOnCursor())) player.setItemOnCursor(null);
        }
    }
}
