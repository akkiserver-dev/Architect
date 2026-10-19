package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import jp.akkiserver.otogi.core.stats.PlayerStats;
import jp.akkiserver.otogi.core.stats.PlayerStatsService;

import java.util.List;
import java.util.Locale;

/** Displays lifetime stats for the sender, or a named player for operators. */
public final class StatsCommand implements SubCommand {
    private final PlayerStatsService stats;

    public StatsCommand(PlayerStatsService stats) {
        this.stats = stats;
    }

    @Override public String getName() { return "stats"; }

    @Override
    public void onSubCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("コンソールからは /otogi stats <player> を使ってください。", NamedTextColor.YELLOW));
                return;
            }
            send(sender, player.getName(), stats.get(player.getUniqueId()));
            return;
        }
        if (!sender.hasPermission("otogi.admin")) {
            sender.sendMessage(Component.text("他のプレイヤーの戦績を見るには運営権限が必要です。", NamedTextColor.RED));
            return;
        }
        Player target = sender.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("オンラインプレイヤーが見つかりません。", NamedTextColor.RED));
            return;
        }
        send(sender, target.getName(), stats.get(target.getUniqueId()));
    }

    private static void send(CommandSender sender, String name, PlayerStats value) {
        sender.sendMessage(Component.text("===== " + name + " の戦績 =====", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("撃破 " + value.kills() + "  /  死亡 " + value.deaths()
                + "  /  試合 " + value.matches() + "  /  勝利 " + value.wins(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text(String.format(Locale.ROOT, "K/D %.2f", value.getKillDeathRatio()), NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length != 1 || !sender.hasPermission("otogi.admin")) return List.of();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return sender.getServer().getOnlinePlayers().stream().map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
