package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import jp.akkiserver.otogi.core.lobby.LobbyMenu;

import java.util.List;

/** Opens the explicit lobby navigation menu. */
public final class MenuCommand implements SubCommand {
    private final LobbyMenu menu;

    public MenuCommand(LobbyMenu menu) {
        this.menu = menu;
    }

    @Override public String getName() { return "menu"; }

    @Override
    public void onSubCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("ロビーメニューはプレイヤーのみ使用できます。", NamedTextColor.RED));
            return;
        }
        menu.open(player);
    }

    @Override public List<String> onTabComplete(CommandSender sender, String[] args) { return List.of(); }
}
