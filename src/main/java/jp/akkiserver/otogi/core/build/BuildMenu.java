package jp.akkiserver.otogi.core.build;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.StaticGuiElement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Park;
import jp.akkiserver.otogi.api.architect.Passive;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tool;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.build.Build;
import jp.akkiserver.otogi.api.build.Builds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The inventory menu where players pick their architects before a match.
 * Every architect is listed once. Left click picks it, right click removes it.
 */
public final class BuildMenu {
    private static final String[] LAYOUT = {
            "ggggggggg",
            "ggggggggg",
            "ggggggggg",
            "ggggggggg",
            "ggggggggg",
            "p h i  rn"
    };

    private final Player player;
    private final Build build;
    private final Map<Architect, StaticGuiElement> elements = new LinkedHashMap<>();
    private final StaticGuiElement status = new StaticGuiElement('i', new ItemStack(Material.BOOK));
    private InventoryGui gui;

    private BuildMenu(Player player) {
        this.player = player;
        this.build = Builds.get(player.getUniqueId());
    }

    /**
     * Opens the build menu. Does nothing while the player is in a running match.
     */
    public static void open(@NotNull Player player) {
        if (isPlaying(player)) {
            player.sendMessage(Component.text("試合中はビルドを変更できません。", NamedTextColor.RED));
            return;
        }
        new BuildMenu(player).show();
    }

    private void show() {
        InventoryGui.InventoryCreator creator = new InventoryGui.InventoryCreator(
                (menu, viewer, type) -> Bukkit.createInventory(menu.getOwner(), type, Component.text(menu.getTitle())),
                (menu, viewer, size) -> Bukkit.createInventory(menu.getOwner(), size, Component.text(menu.getTitle())));
        gui = new InventoryGui(OtogiAPI.plugin.getPlugin(), creator, player, "ビルドを選択", LAYOUT);
        gui.setSilent(true);
        gui.setCloseAction(close -> false);

        GuiElementGroup group = new GuiElementGroup('g');
        List<Architect> entries = Builds.sorted(OtogiAPI.getArchitects());
        for (Architect architect : entries) {
            StaticGuiElement element = new StaticGuiElement('g', new ItemStack(Material.STONE), click -> {
                if (click.getType().isLeftClick() || click.getType().isRightClick()) {
                    onClick(architect, click.getType().isRightClick());
                }
                return true;
            });
            elements.put(architect, element);
            group.addElement(element);
        }
        gui.addElement(group);
        gui.addElement(status);
        gui.addElement(page('p', "前のページ", GuiPageElement.PageAction.PREVIOUS));
        gui.addElement(new StaticGuiElement('h', named(Material.OAK_DOOR,
                Component.text("ロビーへ戻る", NamedTextColor.AQUA)), click -> {
            player.closeInventory();
            Bukkit.dispatchCommand(player, "otogi menu");
            return true;
        }));
        gui.addElement(page('n', "次のページ", GuiPageElement.PageAction.NEXT));
        gui.addElement(new StaticGuiElement('r', named(Material.BARRIER, Component.text("選択をすべて外す", NamedTextColor.RED)), click -> {
            if (!checkPlaying() && !build.isEmpty()) {
                build.clear();
                changed();
            }
            return true;
        }));
        refresh();
        gui.show(player);
        sound(Sound.UI_BUTTON_CLICK, 1.1f);
    }

    private GuiPageElement page(char key, String label, GuiPageElement.PageAction action) {
        GuiPageElement element = new GuiPageElement(key, named(Material.ARROW,
                Component.text(label, NamedTextColor.AQUA)), action);
        var changePage = element.getAction(player);
        element.setAction(click -> {
            if (checkPlaying()) {
                return true;
            }
            sound(Sound.UI_BUTTON_CLICK, 1.0f);
            return changePage.onClick(click);
        });
        return element;
    }

    private void onClick(Architect architect, boolean right) {
        if (checkPlaying()) {
            return;
        }
        if (right) {
            if (!build.remove(architect)) {
                return;
            }
        } else if (build.has(architect)) {
            player.sendMessage(Component.text("外すには右クリックしてください。", NamedTextColor.RED));
            sound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f);
            return;
        } else {
            String reason = Builds.add(build, architect);
            if (reason != null) {
                player.sendMessage(Component.text(reason, NamedTextColor.RED));
                sound(Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f);
                return;
            }
        }
        changed();
    }

    private void changed() {
        Builds.save();
        sound(Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f);
        refresh();
        gui.draw();
    }

    private boolean checkPlaying() {
        if (!isPlaying(player)) {
            return false;
        }
        player.closeInventory();
        player.sendMessage(Component.text("試合中はビルドを変更できません。", NamedTextColor.RED));
        return true;
    }

    private void refresh() {
        elements.forEach((architect, element) -> {
            boolean selected = build.has(architect);
            element.setItem(entryItem(architect, selected, selected ? null : Builds.check(build, architect)));
        });
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("左クリックで選択 / 右クリックで解除", NamedTextColor.GRAY));
        lore.add(Component.empty());
        for (Architect architect : build.getArchitects()) {
            lore.add(Component.text("・", NamedTextColor.GRAY).append(architect.getName().color(NamedTextColor.WHITE)));
        }
        if (build.isEmpty()) {
            lore.add(Component.text("未選択の場合はランダムになります", NamedTextColor.GRAY));
        }
        status.setItem(named(Material.BOOK, Component.text("選択中 " + build.size() + " / " + Builds.getSize(), NamedTextColor.AQUA), lore.toArray(Component[]::new)));
    }

    private static boolean isPlaying(Player player) {
        ArenaPlayer arenaPlayer = OtogiAPI.getArenaPlayer(player);
        return arenaPlayer != null && arenaPlayer.getArena().isRunning();
    }

    private void sound(Sound sound, float pitch) {
        player.playSound(player.getLocation(), sound, 0.35f, pitch);
    }

    // ---- items ----

    private static ItemStack entryItem(Architect architect, boolean selected, @Nullable String blockedReason) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(kind(architect), NamedTextColor.DARK_AQUA));
        architect.getDescription().forEach(line -> lore.add(line.color(NamedTextColor.GRAY)));
        if (architect instanceof Skill skill) {
            lore.add(Component.text("クールダウン " + formatSeconds(skill.getDefaultCoolTime()) + "秒", NamedTextColor.DARK_AQUA));
        }
        lore.add(Component.empty());
        if (selected) {
            lore.add(Component.text("選択中 (右クリックで外す)", NamedTextColor.GREEN));
        } else if (blockedReason != null) {
            lore.add(Component.text(blockedReason, NamedTextColor.RED));
        } else {
            lore.add(Component.text("左クリックで選択", NamedTextColor.DARK_GRAY));
        }
        boolean blocked = !selected && blockedReason != null;
        ItemStack item = named(icon(architect), Component.text(selected ? "✓ " : "")
                .append(architect.getName()).color(selected ? NamedTextColor.GREEN : blocked ? NamedTextColor.RED : NamedTextColor.WHITE),
                lore.toArray(Component[]::new));
        ItemMeta meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(selected);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatSeconds(int ticks) {
        return java.math.BigDecimal.valueOf(ticks).divide(java.math.BigDecimal.valueOf(20)).stripTrailingZeros().toPlainString();
    }

    private static String kind(Architect architect) {
        if (architect instanceof Park) {
            return "式";
        }
        if (architect instanceof Passive) {
            return "相";
        }
        if (architect instanceof Skill) {
            return "術";
        }
        return architect instanceof Tool ? "具" : "";
    }

    private static Material icon(Architect architect) {
        if (architect instanceof Skill skill) {
            return skill.getIcon();
        }
        if (architect instanceof Tool tool) {
            return tool.createItem().getType();
        }
        return architect instanceof Park ? Material.AMETHYST_CLUSTER : Material.ECHO_SHARD;
    }

    private static ItemStack named(Material material, Component name, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.stream(lore).map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
