package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.arena.ArenaPlayer;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.build.Build;
import jp.akkiserver.otogi.api.build.Builds;
import jp.akkiserver.otogi.core.build.BuildMenu;

import java.util.List;
import java.util.Locale;

/** /otogi build [show|reset] */
public final class BuildCommand implements SubCommand {
    private static final List<String> ACTIONS = List.of("show", "reset");

    @Override
    public String getName() {
        return "build";
    }

    @Override
    public void onSubCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("プレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }
        String action = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        Build build = Builds.get(player.getUniqueId());
        switch (action) {
            case "" -> BuildMenu.open(player);
            case "show" -> {
                player.sendMessage(Component.text("===== ビルド " + build.size() + " / " + Builds.getSize() + " =====", NamedTextColor.GOLD));
                if (build.isEmpty()) {
                    player.sendMessage(Component.text("未選択 (試合開始時にランダム)", NamedTextColor.YELLOW));
                }
                for (Architect architect : build.getArchitects()) {
                    player.sendMessage(architect.getName().color(NamedTextColor.WHITE));
                }
            }
            case "reset" -> {
                ArenaPlayer arenaPlayer = OtogiAPI.getArenaPlayer(player);
                if (arenaPlayer != null && arenaPlayer.getArena().isRunning()) {
                    player.sendMessage(Component.text("試合中はビルドを変更できません。", NamedTextColor.RED));
                    return;
                }
                build.clear();
                Builds.save();
                player.sendMessage(Component.text("ビルドを空にしました。(試合開始時にランダム)", NamedTextColor.GREEN));
            }
            default -> player.sendMessage(Component.text("/otogi build [show|reset]", NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return ACTIONS.stream().filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
