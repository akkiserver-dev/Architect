package jp.akkiserver.otogi.core.command;

import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Park;
import jp.akkiserver.otogi.api.architect.Skill;
import jp.akkiserver.otogi.api.architect.Tool;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import jp.akkiserver.otogi.OtogiPlugin;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.*;
import jp.akkiserver.otogi.core.script.ScriptArchitect;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Operator inspection and dry validation for data-driven editing. */
public final class ArchitectCommand implements SubCommand {
    private final OtogiPlugin plugin;
    public ArchitectCommand(OtogiPlugin plugin) { this.plugin = plugin; }
    @Override public String getName() { return "architect"; }
    @Override public boolean canUse(CommandSender sender) { return sender.hasPermission("otogi.admin"); }

    @Override public void onSubCommand(CommandSender sender, String[] args) {
        try {
            String operation = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
            if (operation.equals("validate")) {
                sender.sendMessage(Component.text("定義検証成功: " + plugin.validateArchitects() + "件。まだ反映していません。", NamedTextColor.GREEN));
            } else if (operation.equals("info")) {
                if (args.length < 2) throw new IllegalArgumentException("/otogi architect info <id>");
                String id = args[1].contains(":") ? args[1] : "otogi:" + args[1];
                Architect architect = OtogiAPI.getArchitect(id);
                if (architect == null) throw new IllegalArgumentException("未登録のアーキテクト: " + id);
                sender.sendMessage(architect.getName().color(NamedTextColor.GOLD)
                        .append(Component.text(" / " + architect.getId() + " / " + kind(architect), NamedTextColor.GRAY)));
                architect.getDescription().forEach(sender::sendMessage);
                if (architect instanceof Skill skill) sender.sendMessage(Component.text("CT: " + skill.getDefaultCoolTime() + "tick", NamedTextColor.AQUA));
                if (architect instanceof ScriptArchitect scripted) {
                    var definition = scripted.getDefinition();
                    sender.sendMessage(Component.text("JSON backend=" + definition.backend() + " / "
                            + (definition.backend().equals("lua") ? definition.script() : definition.handler()), NamedTextColor.AQUA));
                    sender.sendMessage(Component.text("parameters: " + definition.parameters(), NamedTextColor.GRAY));
                }
            } else if (operation.equals("list")) {
                var all = OtogiAPI.getArchitects().stream().sorted(Comparator.comparing(architect -> architect.getId().toString())).toList();
                int pages = Math.max(1, (all.size() + 15) / 16);
                int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                if (page < 1 || page > pages) throw new IllegalArgumentException("ページは1〜" + pages);
                sender.sendMessage(Component.text("登録 " + all.size() + "件 / " + page + "/" + pages + "ページ", NamedTextColor.GOLD));
                for (Architect architect : all.stream().skip((long) (page - 1) * 16).limit(16).toList())
                    sender.sendMessage(Component.text(architect.getId() + "  ", NamedTextColor.GRAY).append(architect.getName()));
            } else throw new IllegalArgumentException("/otogi architect list [page] | info <id> | validate");
        } catch (RuntimeException exception) {
            sender.sendMessage(Component.text("定義の確認に失敗: " + exception.getMessage(), NamedTextColor.RED));
        }
    }

    private static String kind(Architect architect) {
        return architect instanceof Skill ? "術" : architect instanceof Tool ? "具" : architect instanceof Park ? "式" : "相";
    }
    @Override public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> options = args.length == 1 ? List.of("list", "info", "validate") : args.length == 2 && args[0].equalsIgnoreCase("info")
                ? OtogiAPI.getArchitects().stream().map(architect -> architect.getId().toString()).sorted().toList() : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(prefix)).toList();
    }
}
