package jp.akkiserver.otogi.core.lobby;

import de.themoep.inventorygui.GuiElementGroup;
import de.themoep.inventorygui.GuiPageElement;
import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.core.game.MatchService;
import jp.akkiserver.otogi.core.stage.MapDefinition;
import jp.akkiserver.otogi.core.stats.PlayerStats;
import jp.akkiserver.otogi.core.stats.PlayerStatsService;

import java.util.ArrayList;
import java.util.List;

/** Lobby navigation opened from a command or the menu compass. */
public final class LobbyMenu {
    private final JavaPlugin plugin;
    private final MatchService matches;
    private final PlayerStatsService stats;

    public LobbyMenu(@NotNull JavaPlugin plugin, @NotNull MatchService matches, @NotNull PlayerStatsService stats) {
        this.plugin = plugin;
        this.matches = matches;
        this.stats = stats;
    }

    public void open(@NotNull Player player) {
        String[] layout = {"         ", " b s h m ", " q x a   "};
        InventoryGui gui = create(player, "御伽のロビー", layout);
        gui.addElement(button('b', Material.WRITABLE_BOOK, "ビルド", "使う式・相・術・具を選びます。", click -> { command(player, "otogi build"); return true; }));
        gui.addElement(button('s', Material.PAPER, "自分の戦績", "通算撃破・死亡・試合数・勝利数", click -> { openStats(player); return true; }));
        gui.addElement(button('h', Material.KNOWLEDGE_BOOK, "遊び方・操作", "試合中の操作とコマンドを確認します。", click -> { openHelp(player); return true; }));
        gui.addElement(button('m', Material.FILLED_MAP, "マップ・モード", "管理者は使用する内容を選べます。", click -> { openMaps(player); return true; }));
        gui.addElement(button('q', queueIcon(player), queueName(player), queueLore(player), click -> { toggleQueue(player); return true; }));
        gui.addElement(button('x', Material.CLOCK, "試合状況", statusLore(), click -> { showStatus(player); return true; }));
        if (player.hasPermission("otogi.admin")) {
            gui.addElement(button('a', matches.getActiveArena() == null ? Material.LIME_CONCRETE : Material.RED_CONCRETE,
                    matches.getActiveArena() == null ? "待機者で試合開始" : "試合を終了", adminActionLore(), click -> { adminMatchAction(player); return true; }));
        } else {
            gui.addElement(button('a', Material.GRAY_DYE, "運営メニュー", "運営だけがマップ選択と試合開始を行えます。", click -> {
                player.sendMessage(Component.text("この操作には運営権限が必要です。", NamedTextColor.RED));
                return true;
            }));
        }
        gui.show(player);
    }

    private void openStats(Player player) {
        PlayerStats value = stats.get(player);
        List<Component> lines = List.of(
                Component.text("撃破: " + value.kills(), NamedTextColor.RED),
                Component.text("死亡: " + value.deaths(), NamedTextColor.GRAY),
                Component.text("試合: " + value.matches(), NamedTextColor.AQUA),
                Component.text("勝利: " + value.wins(), NamedTextColor.GOLD),
                Component.text(String.format(java.util.Locale.ROOT, "K/D: %.2f", value.getKillDeathRatio()), NamedTextColor.WHITE));
        InventoryGui gui = create(player, "あなたの戦績", new String[]{"         ", "    s    ", "    b    "});
        gui.addElement(item('s', Material.GOLD_INGOT, "通算記録", lines.toArray(Component[]::new)));
        gui.addElement(button('b', Material.ARROW, "ロビーへ戻る", "", click -> { open(player); return true; }));
        gui.show(player);
    }

    private void openHelp(Player player) {
        List<Component> lore = new ArrayList<>(List.of(
                Component.text("ロビーでビルドを選び、待機ボタンで参加予約します。", NamedTextColor.GRAY),
                Component.text("待機は自動参加ではありません。もう一度押すと退出します。", NamedTextColor.GRAY),
                Component.text("試合では武器で攻撃し、選んだ術を発動します。", NamedTextColor.GRAY),
                Component.text("術の操作は各ビルド項目とアイテム説明を確認してください。", NamedTextColor.GRAY),
                Component.empty(),
                Component.text("/otogi menu  ロビーを開く", NamedTextColor.AQUA),
                Component.text("/otogi build  ビルドを選ぶ", NamedTextColor.AQUA),
                Component.text("/otogi stats  戦績を確認", NamedTextColor.AQUA)));
        if (player.hasPermission("otogi.admin")) {
            lore.add(Component.empty());
            lore.add(Component.text("運営: マップ/モードを選び、待機者を集めて開始します。", NamedTextColor.GOLD));
            lore.add(Component.text("/otogi game status  状況確認", NamedTextColor.YELLOW));
            lore.add(Component.text("/otogi reload  JSON・Lua設定を再読込", NamedTextColor.YELLOW));
            lore.add(Component.text("マップ定義はJSON、拡張動作はLuaで管理します。", NamedTextColor.GRAY));
        }
        InventoryGui gui = create(player, "遊び方・操作", new String[]{"         ", "    h    ", "    b    "});
        gui.addElement(item('h', Material.KNOWLEDGE_BOOK, "基本操作", lore.toArray(Component[]::new)));
        gui.addElement(button('b', Material.ARROW, "ロビーへ戻る", "", click -> { open(player); return true; }));
        gui.show(player);
    }

    private void openMaps(Player player) {
        String[] layout = {"mmmmmmmmm", "mmmmmmmmm", "mmmmmmmmm", "mmmmmmmmm", "p   s  nb"};
        InventoryGui gui = create(player, "マップを選択", layout);
        GuiElementGroup group = new GuiElementGroup('m');
        String selected = matches.getSelectedMapId();
        List<MapDefinition> definitions = List.copyOf(matches.getMaps().getDefinitions());
        if (definitions.isEmpty()) {
            group.addElement(new StaticGuiElement('m', itemStack(Material.BOOK,
                    Component.text("マップは未登録です", NamedTextColor.YELLOW),
                    Component.text("運営は既存ワールドを /otogi map create <id> で登録してください。", NamedTextColor.GRAY),
                    Component.text("登録後、JSONを編集して /otogi reload を実行します。", NamedTextColor.GRAY)), click -> true));
        }
        for (MapDefinition map : definitions) {
            String title = map.title() == null || map.title().isBlank() ? map.id() : map.title();
            List<Component> lore = new ArrayList<>();
            if (map.description() != null && !map.description().isBlank()) lore.add(Component.text(map.description(), NamedTextColor.GRAY));
            lore.add(Component.text("ID: " + map.id(), NamedTextColor.DARK_GRAY));
            lore.add(Component.text(map.id().equals(selected) ? "選択中" : "クリックで詳細", map.id().equals(selected) ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            group.addElement(new StaticGuiElement('m', itemStack(Material.FILLED_MAP,
                    Component.text((map.id().equals(selected) ? "✓ " : "") + title, NamedTextColor.WHITE), lore.toArray(Component[]::new)), click -> {
                if (!player.hasPermission("otogi.admin")) {
                    player.sendMessage(Component.text("マップ選択には運営権限が必要です。", NamedTextColor.RED));
                    return true;
                }
                try {
                    matches.selectMap(map.id());
                    player.sendMessage(Component.text("マップを「" + title + "」にしました。", NamedTextColor.GREEN));
                    openMaps(player);
                } catch (RuntimeException exception) {
                    player.sendMessage(Component.text("マップを選択できませんでした: " + exception.getMessage(), NamedTextColor.RED));
                }
                return true;
            }));
        }
        gui.addElement(group);
        gui.addElement(page('p', "前のページ", GuiPageElement.PageAction.PREVIOUS));
        gui.addElement(page('n', "次のページ", GuiPageElement.PageAction.NEXT));
        gui.addElement(button('s', Material.COMPASS, "モードを選ぶ", "登録済みのモードを表示します。", click -> { openModes(player); return true; }));
        gui.addElement(button('b', Material.ARROW, "ロビーへ戻る", "", click -> { open(player); return true; }));
        gui.show(player);
    }

    private void openModes(Player player) {
        String[] layout = {"mmmmmmmmm", "mmmmmmmmm", "mmmmmmmmm", "mmmmmmmmm", "         ", "p       nb"};
        InventoryGui gui = create(player, "モードを選択", layout);
        GuiElementGroup group = new GuiElementGroup('m');
        String selected = matches.getSelectedModeId();
        for (Mode mode : OtogiAPI.getModes()) {
            String id = mode.getId().toString();
            Component title = mode.getName();
            if (title == null) title = Component.text(id);
            List<Component> lore = new ArrayList<>();
            if (mode.getDescription() != null) lore.add(mode.getDescription().color(NamedTextColor.GRAY));
            lore.add(Component.text("ID: " + id, NamedTextColor.DARK_GRAY));
            lore.add(Component.text(id.equals(selected) ? "選択中" : "クリックで選択", id.equals(selected) ? NamedTextColor.GREEN : NamedTextColor.GRAY));
            group.addElement(new StaticGuiElement('m', itemStack(Material.ENDER_EYE,
                    title.append(Component.text(id.equals(selected) ? " ✓" : "", NamedTextColor.GREEN)), lore.toArray(Component[]::new)), click -> {
                if (!player.hasPermission("otogi.admin")) {
                    player.sendMessage(Component.text("モード選択には運営権限が必要です。", NamedTextColor.RED));
                    return true;
                }
                matches.selectMode(id);
                player.sendMessage(Component.text("モードを「" + id + "」にしました。", NamedTextColor.GREEN));
                openModes(player);
                return true;
            }));
        }
        gui.addElement(group);
        gui.addElement(page('p', "前のページ", GuiPageElement.PageAction.PREVIOUS));
        gui.addElement(page('n', "次のページ", GuiPageElement.PageAction.NEXT));
        gui.addElement(button('b', Material.ARROW, "マップ選択へ戻る", "", click -> { openMaps(player); return true; }));
        gui.show(player);
    }

    private void toggleQueue(Player player) {
        Arena active = matches.getActiveArena();
        if (active != null && active.getPlayer(player) != null) {
            if (matches.leave(player)) {
                player.sendMessage(Component.text("試合から退出しました。今回の試合は勝利扱いになりません。", NamedTextColor.YELLOW));
                open(player);
            }
            return;
        }
        boolean queued = matches.getQueuedPlayers().stream().anyMatch(candidate -> candidate.getUniqueId().equals(player.getUniqueId()));
        boolean changed = queued ? matches.leave(player) : matches.join(player);
        if (changed) {
            player.sendMessage(Component.text(queued ? "参加待機から退出しました。" : "参加待機に登録しました。", queued ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
            open(player);
        } else {
            player.sendMessage(Component.text(matches.getActiveArena() != null
                    ? "試合中は途中参加できません。次の試合に参加待機してください。"
                    : "現在の状態では待機を変更できません。", NamedTextColor.RED));
        }
    }

    private void adminMatchAction(Player player) {
        if (!player.hasPermission("otogi.admin")) return;
        Arena active = matches.getActiveArena();
        if (active != null) {
            matches.stop();
            player.sendMessage(Component.text("試合を終了しました。", NamedTextColor.YELLOW));
            open(player);
            return;
        }
        List<Player> participants = List.copyOf(matches.getQueuedPlayers());
        if (participants.size() < 2) {
            player.sendMessage(Component.text("開始には待機中のプレイヤーが2人以上必要です。", NamedTextColor.RED));
            return;
        }
        try {
            if (matches.getSelectedMapId() == null) {
                player.sendMessage(Component.text("先に既存ワールドを /otogi map create <id> で登録してください。", NamedTextColor.RED));
                return;
            }
            Arena arena = matches.start(matches.getSelectedModeId(), matches.getSelectedMapId(), participants);
            player.sendMessage(Component.text("待機者 " + arena.getPlayers().size() + "人で試合を開始しました。", NamedTextColor.GREEN));
            player.closeInventory();
        } catch (RuntimeException exception) {
            player.sendMessage(Component.text("試合を開始できませんでした: " + exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void showStatus(Player player) {
        Arena arena = matches.getActiveArena();
        if (arena == null) {
            player.sendMessage(Component.text("試合はありません。待機者: " + matches.getQueuedPlayers().size() + "人", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("試合中: " + arena.getMode().getId() + " / 参加者 " + arena.getPlayers().size() + "人 / 残り " + arena.getMatchTimer().getRemainingMatchTime() + "秒", NamedTextColor.AQUA));
        }
    }

    private List<Component> statusLore() {
        Arena arena = matches.getActiveArena();
        if (arena == null) return List.of(Component.text("試合なし", NamedTextColor.GRAY), Component.text("待機者 " + matches.getQueuedPlayers().size() + "人", NamedTextColor.AQUA));
        return List.of(Component.text("参加者 " + arena.getPlayers().size() + "人", NamedTextColor.AQUA), Component.text("残り " + arena.getMatchTimer().getRemainingMatchTime() + "秒", NamedTextColor.GRAY));
    }

    private List<Component> adminActionLore() {
        return matches.getActiveArena() == null
                ? List.of(Component.text("待機者のみが参加します。", NamedTextColor.GRAY), Component.text("待機者: " + matches.getQueuedPlayers().size() + "人", NamedTextColor.AQUA))
                : List.of(Component.text("現在の試合を終了します。", NamedTextColor.GRAY));
    }

    private Material queueIcon(Player player) {
        if (isActiveParticipant(player)) return Material.BARRIER;
        if (matches.getActiveArena() != null) return Material.GRAY_DYE;
        return isQueued(player) ? Material.RED_DYE : Material.LIME_DYE;
    }
    private String queueName(Player player) {
        if (isActiveParticipant(player)) return "試合から退出";
        if (matches.getActiveArena() != null) return "試合中（待機受付なし）";
        return isQueued(player) ? "待機から退出" : "試合に参加待機";
    }
    private List<Component> queueLore(Player player) {
        if (isActiveParticipant(player)) {
            return List.of(Component.text("クリックするとこの試合から退出します。", NamedTextColor.GRAY),
                    Component.text("退出後は今回の試合で勝利扱いになりません。", NamedTextColor.RED));
        }
        if (matches.getActiveArena() != null) {
            return List.of(Component.text("試合が終わると次の試合に参加待機できます。", NamedTextColor.GRAY),
                    Component.text("待機者 " + matches.getQueuedPlayers().size() + "人", NamedTextColor.AQUA));
        }
        return List.of(Component.text(isQueued(player) ? "クリックすると待機を解除します。" : "クリックすると次の試合に参加します。", NamedTextColor.GRAY), Component.text("待機者 " + matches.getQueuedPlayers().size() + "人", NamedTextColor.AQUA));
    }
    private boolean isQueued(Player player) { return matches.getQueuedPlayers().stream().anyMatch(candidate -> candidate.getUniqueId().equals(player.getUniqueId())); }
    private boolean isActiveParticipant(Player player) {
        Arena arena = matches.getActiveArena();
        return arena != null && arena.getPlayer(player) != null;
    }

    private InventoryGui create(Player player, String title, String[] layout) {
        InventoryGui.InventoryCreator creator = new InventoryGui.InventoryCreator(
                (menu, viewer, type) -> Bukkit.createInventory(menu.getOwner(), type, Component.text(menu.getTitle())),
                (menu, viewer, size) -> Bukkit.createInventory(menu.getOwner(), size, Component.text(menu.getTitle())));
        InventoryGui gui = new InventoryGui(plugin, creator, player, title, layout);
        gui.setSilent(true);
        gui.setCloseAction(close -> false);
        return gui;
    }

    private GuiPageElement page(char key, String label, GuiPageElement.PageAction action) {
        return new GuiPageElement(key, named(Material.ARROW, Component.text(label, NamedTextColor.AQUA)), action);
    }

    private StaticGuiElement button(char key, Material icon, String title, String lore, de.themoep.inventorygui.GuiElement.Action action) {
        return button(key, icon, title, List.of(Component.text(lore, NamedTextColor.GRAY)), action);
    }

    private StaticGuiElement button(char key, Material icon, String title, List<Component> lore, de.themoep.inventorygui.GuiElement.Action action) {
        return new StaticGuiElement(key, itemStack(icon, Component.text(title, NamedTextColor.GOLD), lore.toArray(Component[]::new)), action);
    }

    private StaticGuiElement item(char key, Material icon, String title, Component... lore) {
        return new StaticGuiElement(key, itemStack(icon, Component.text(title, NamedTextColor.GOLD), lore), click -> true);
    }

    private ItemStack named(Material material, Component title) { return itemStack(material, title); }

    private ItemStack itemStack(Material material, Component title, Component... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.Arrays.stream(lore).map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void command(Player player, String command) { Bukkit.dispatchCommand(player, command); }
}
