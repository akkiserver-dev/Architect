package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.Arena;
import jp.akkiserver.otogi.api.arena.mode.Mode;
import jp.akkiserver.otogi.core.game.MatchService;

import java.util.List;
import java.util.Locale;

/** Operator commands backed by the shared single-match service. */
public final class GameCommand implements SubCommand {
    private final MatchService matches;
    private static void send(CommandSender sender, String text) { sender.sendMessage(Component.text(text)); }
    private static void send(CommandSender sender, Component text) { sender.sendMessage(text); }

    public GameCommand(@NotNull MatchService matches) {
        this.matches = matches;
    }

    @Override public @NotNull String getName() { return "game"; }

    @Override
    public void onSubCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, Component.text("/otogi game join | leave | modes | status\n管理者: start [mode] [map] | mode <id> | stop", NamedTextColor.YELLOW));
            return;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "start" -> start(sender, args);
            case "stop" -> stop(sender);
            case "status" -> status(sender);
            case "join" -> join(sender, args);
            case "leave" -> {
                if (!(sender instanceof Player player)) { send(sender, "ゲーム内プレイヤー専用です。"); return; }
                send(sender, matches.leave(player) ? "参加を取り消しました。" : "参加していません。");
            }
            case "modes" -> OtogiAPI.getModes().forEach(mode -> send(sender, Component.text(mode.getId().getKey() + ": ")
                    .append(mode.getName()).append(Component.text(" — ")).append(mode.getDescription())));
            case "mode" -> {
                if (!sender.hasPermission("otogi.admin")) { send(sender, "管理者権限が必要です。"); return; }
                try {
                    if (args.length < 2) throw new IllegalArgumentException("/otogi game mode <id>");
                    if (matches.getActiveArena() != null) throw new IllegalStateException("試合終了後に選択してください。");
                    matches.selectMode(args[1]); send(sender, "次のモード: " + matches.getSelectedModeId());
                } catch (RuntimeException exception) { send(sender, Component.text(exception.getMessage(), NamedTextColor.RED)); }
            }
            default -> send(sender, Component.text("不明なゲーム操作です。/otogi help を確認してください。", NamedTextColor.RED));
        }
    }

    private void join(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { send(sender, "ゲーム内プレイヤー専用です。"); return; }
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("all")) {
                send(sender, "すべてのプレイヤーを参加させました。");
                Bukkit.getOnlinePlayers().forEach(matches::join);
            }
        } else {
            send(sender, matches.join(player) ? "参加待機に登録しました。開始までビルドを準備できます。" : "登録済み、または試合進行中です。");
        }
    }

    private void start(CommandSender sender, String[] args) {
        if (!sender.hasPermission("otogi.admin")) {
            send(sender, Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        if (matches.getActiveArena() != null) {
            send(sender, Component.text("試合は既に進行中です。", NamedTextColor.RED));
            return;
        }
        if (args.length >= 2) {
            try {
                matches.selectMode(args[1]);
            } catch (IllegalArgumentException exception) {
                send(sender, Component.text(exception.getMessage(), NamedTextColor.RED));
                return;
            }
        }
        List<Player> participants = List.copyOf(matches.getQueuedPlayers());
        try {
            Arena arena = matches.start(matches.getSelectedModeId(), args.length > 2 ? args[2] : matches.getSelectedMapId(), participants);
            send(sender, Component.text("待機中の " + arena.getPlayers().size() + " 人で試合を開始しました。", NamedTextColor.GREEN));
        } catch (RuntimeException exception) {
            send(sender, Component.text("試合を開始できませんでした: " + exception.getMessage(), NamedTextColor.RED));
        }
    }

    private void stop(CommandSender sender) {
        if (!sender.hasPermission("otogi.admin")) {
            send(sender, Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        send(sender, matches.stop()
                ? Component.text("試合を終了しました。", NamedTextColor.YELLOW)
                : Component.text("進行中の試合はありません。", NamedTextColor.GRAY));
    }

    private void status(CommandSender sender) {
        Arena arena = matches.getActiveArena();
        if (arena == null) {
            send(sender, Component.text("試合はありません。参加待機: " + matches.getQueuedPlayers().size() + "人", NamedTextColor.GRAY));
        } else {
            send(sender, Component.text("参加者 " + arena.getPlayers().size() + "人 / 残り "
                    + arena.getMatchTimer().getRemainingMatchTime() + "秒 / " + arena.getMode().getId(), NamedTextColor.AQUA));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("join", "leave", "modes", "mode", "start", "stop", "status").stream()
                    .filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("mode"))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return OtogiAPI.getModes().stream().map(Mode::getId).map(NamespacedKey::toString)
                    .filter(value -> value.startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return matches.getMaps().getDefinitions().stream().map(map -> map.id())
                    .filter(value -> value.startsWith(args[2].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
