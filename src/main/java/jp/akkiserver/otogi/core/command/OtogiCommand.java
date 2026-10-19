package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.OtogiPlugin;
import jp.akkiserver.otogi.core.lobby.LobbyMenu;

import java.util.List;
import java.util.function.Consumer;

public final class OtogiCommand extends CommandBase {
    private final LobbyMenu lobby;

    public OtogiCommand(@NotNull OtogiPlugin plugin) {
        this.lobby = plugin.getLobbyMenu();
        setSubCommands(List.of(
                new GameCommand(plugin.getMatchService()),
                new BuildCommand(),
                new MapCommand(plugin.getMatchService()),
                new ArchitectCommand(plugin),
                new MenuCommand(lobby),
                new StatsCommand(plugin.getPlayerStats()),
                new SimpleCommand("help", OtogiCommand::help),
                new SimpleCommand("version", sender -> send(sender, "otogi.command.version", Component.text("version " + plugin.getPluginMeta().getVersion()))),
                new SimpleCommand("reload", "otogi.admin", sender -> {
                    try {
                        plugin.reloadSettings();
                        send(sender, "otogi.command.reload", Component.text("設定・JSON・Luaを再読み込みしました。"));
                    } catch (RuntimeException exception) {
                        sender.sendMessage(Component.text("再読み込みできませんでした: " + exception.getMessage(), NamedTextColor.RED));
                    }
                })
        ));
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof org.bukkit.entity.Player player) {
                // Keep the normal inventory untouched until the player explicitly opens the menu.
                lobby.open(player);
            } else {
                help(sender);
            }
            return true;
        }
        return super.onCommand(sender, command, label, args);
    }

    private static void help(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("御伽 コマンド", NamedTextColor.GOLD));
        helpLine(sender, "/otogi menu", "ロビーメニューを開く");
        helpLine(sender, "/otogi build", "ビルドを選択");
        helpLine(sender, "/otogi build show", "選択中のビルドを確認");
        helpLine(sender, "/otogi build reset", "選択をすべて解除");
        helpLine(sender, "/otogi stats", "自分の通算戦績を確認");
        helpLine(sender, "/otogi game join", "次の試合に参加待機");
        helpLine(sender, "/otogi game leave", "待機を取り消す・試合を退出");
        helpLine(sender, "/otogi map list", "登録済みマップを確認");
        helpLine(sender, "/otogi game status", "試合の状況を確認");
        helpLine(sender, "/otogi version", "バージョンを確認");
        if (sender.hasPermission("otogi.admin")) {
            helpLine(sender, "/otogi game start", "待機者で試合を開始");
            helpLine(sender, "/otogi game stop", "試合を終了");
            helpLine(sender, "/otogi menu", "マップとモードを選び、待機者で試合を開始");
            helpLine(sender, "/otogi map create <id>", "今いる既存ワールドをマップとして登録");
            helpLine(sender, "/otogi architect info <id>", "能力の実装方式と調整値を確認");
            helpLine(sender, "/otogi architect validate", "JSON・Luaを反映せずに検証");
            helpLine(sender, "/otogi reload", "設定・JSON・Luaを再読み込み");
        }
    }

    private static void helpLine(CommandSender sender, String command, String description) {
        sender.sendMessage(Component.text(command, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text("クリックで入力欄にコピー", NamedTextColor.GRAY)))
                .append(Component.text(" — " + description, NamedTextColor.GRAY)));
    }


    private record SimpleCommand(String name, String permission, Consumer<CommandSender> action) implements SubCommand {
        private SimpleCommand(String name, Consumer<CommandSender> action) {
            this(name, null, action);
        }

        @Override
        public boolean canUse(CommandSender sender) {
            return permission == null || sender.hasPermission(permission);
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void onSubCommand(@NotNull CommandSender sender, @NotNull String[] args) {
            action.accept(sender);
        }

        @Override
        public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
            return List.of();
        }
    }
}
