package jp.akkiserver.otogi.api.architect;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.util.ArchitectItems;

import java.util.ArrayList;
import java.util.List;

/**
 * An architect the player activates by right clicking its item.
 * The cooldown is tracked per player in {@link ArenaPlayer}.
 */
public interface Skill extends Architect {
    /**
     * Gets the default cooldown duration in ticks.
     */
    int getDefaultCoolTime();

    /**
     * Activates this skill.
     *
     * @return {@code true} if the skill was activated and the cooldown should start
     */
    boolean onUse(@NotNull ArenaPlayer player);

    @NotNull
    default Material getIcon() {
        return Material.NETHER_STAR;
    }

    @NotNull
    default ItemStack createItem() {
        ItemStack item = new ItemStack(getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.itemName(getName().color(NamedTextColor.AQUA));
        List<Component> lore = new ArrayList<>(getDescription());
        lore.add(Component.text("右クリックで発動 / CT " + getDefaultCoolTime() / 20 + "秒", NamedTextColor.GRAY));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return ArchitectItems.mark(item, this);
    }
}
