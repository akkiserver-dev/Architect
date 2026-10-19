package jp.akkiserver.otogi.api.util;

import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;

public final class ArchitectItems {
    private static final NamespacedKey KEY = new NamespacedKey("otogi", "architect");

    @NotNull
    public static ItemStack mark(@NotNull ItemStack item, @NotNull Architect architect) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, architect.getId().toString());
        item.setItemMeta(meta);
        return item;
    }

    @Nullable
    public static Architect read(@Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String id = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return id == null ? null : OtogiAPI.getArchitect(id);
    }

    /** Items in a running match keep that match's architect version across a definition reload. */
    @Nullable
    public static Architect read(@Nullable ItemStack item, @NotNull ArenaPlayer owner) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        if (id == null) return null;
        NamespacedKey key = NamespacedKey.fromString(id);
        if (key == null) return null;
        NamespacedKey canonical = ArchitectIds.canonical(key);
        return owner.getArchitects().stream().filter(architect -> architect.getId().equals(canonical)).findFirst().orElse(null);
    }

    private ArchitectItems() {}
}
