package jp.akkiserver.otogi.core.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    String getName();

    void onSubCommand(CommandSender sender, String[] args);

    List<String> onTabComplete(CommandSender sender, String[] args);

    default boolean canUse(CommandSender sender) {
        return true;
    }
}
