package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class CommandBase implements CommandExecutor, TabCompleter {
    private List<SubCommand> subCommands = List.of();

    protected void setSubCommands(List<SubCommand> subCommands) {
        this.subCommands = List.copyOf(subCommands);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length >= 1) {
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().equalsIgnoreCase(args[0])) {
                    if (!subCommand.canUse(sender)) {
                        send(sender, "otogi.command.no_permission", Component.text("権限がありません。", NamedTextColor.RED));
                        return true;
                    }
                    subCommand.onSubCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                    return true;
                }
            }
        }
        sender.sendMessage(Component.text("コマンドが見つかりません。", NamedTextColor.RED)
                .append(Component.text(" /" + s + " help で使い方を確認", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.runCommand("/" + s + " help"))));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return subCommands.stream().filter(subCommand -> subCommand.canUse(commandSender))
                    .map(SubCommand::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }

        if (args.length >= 2) {
            for (SubCommand subCommand : subCommands) {
                if (subCommand.getName().equalsIgnoreCase(args[0]) && subCommand.canUse(commandSender)) {
                    return subCommand.onTabComplete(commandSender, Arrays.copyOfRange(args, 1, args.length));
                }
            }
        }

        return List.of();
    }

    static void send(@NotNull CommandSender sender, @NotNull String key, @NotNull Component fallback) {
        sender.sendMessage(Component.translatable(key, PlainTextComponentSerializer.plainText().serialize(fallback)).style(fallback.style()));
    }
}
