package jp.akkiserver.otogi.core.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import jp.akkiserver.otogi.core.game.MatchService;
import jp.akkiserver.otogi.core.stage.MapDefinition;
import java.util.List;
import java.util.Locale;

/** Staged metadata setup on terrain already built by the operator. Each edit is saved to JSON. */
public final class MapCommand implements SubCommand {
    private final MatchService matches;
    private static void send(CommandSender sender, String text) { sender.sendMessage(Component.text(text)); }
    private static void send(CommandSender sender, Component text) { sender.sendMessage(text); }
    public MapCommand(MatchService matches) { this.matches = matches; }
    @Override public String getName() { return "map"; }
    @Override public void onSubCommand(CommandSender sender, String[] args) {
        var maps = matches.getMaps();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            if (maps.getDefinitions().isEmpty()) send(sender, "マップは未登録です。自作マップ内で /otogi map create <id> [表示名] から設定してください。");
            maps.getDefinitions().forEach(map -> send(sender, Component.text(
                    (map.id().equals(matches.getSelectedMapId()) ? "✓ " : "  ") + map.id() + " / " + map.title()
                    + " / " + map.world() + " / spawn " + map.spawns().size())));
            help(sender);
            return;
        }
        if (!sender.hasPermission("otogi.admin")) { send(sender, "管理者権限が必要です。"); return; }
        String operation = args[0].toLowerCase(Locale.ROOT);
        try {
            if (operation.equals("help")) { help(sender); return; }
            if (operation.equals("reload")) { matches.reload(); send(sender, "maps/*.json と modes.json を再読込しました。"); return; }
            if (args.length < 2) throw new IllegalArgumentException("マップIDが必要です。/otogi map help");
            String id = args[1];
            if (operation.equals("validate") || operation.equals("info")) {
                var definition = maps.getDefinition(id);
                if (definition == null) throw new IllegalArgumentException("マップがありません: " + id);
                send(sender, definition.title() + " / world=" + definition.world() + " / spawn=" + definition.spawns().size());
                if (definition.controlPoint() != null)
                    send(sender, "拠点: 半径 " + definition.controlRadius() + " ブロック / 直径 " + (definition.controlRadius() * 2) + " ブロック");
                var issues = maps.getValidationIssues(id);
                if (definition.controlPoint() == null) send(sender, Component.text("拠点未設定: FFA・残機戦は利用可。拠点制圧には point の設定が必要です。", NamedTextColor.GRAY));
                if (issues.isEmpty()) { maps.prepare(id); send(sender, Component.text("検証成功。プレイ可能です。地形は変更していません。", NamedTextColor.GREEN)); }
                else { send(sender, Component.text("未設定・修正が必要な項目:", NamedTextColor.YELLOW)); issues.forEach(issue -> send(sender, "・" + issue)); }
                return;
            }
            if (matches.getActiveArena() != null) throw new IllegalStateException("試合終了後にマップ設定を変更してください。");
            switch (operation) {
                case "create" -> {
                    Player player = player(sender);
                    String title = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : id;
                    maps.create(id, title, player.getWorld());
                    if (matches.getSelectedMapId() == null) matches.selectMap(id);
                    send(sender, "マップ定義を作成しました。pos1/pos2 → spawn add → spectator → recovery → point → validate の順に設定してください。");
                }
                case "select" -> { matches.selectMap(id); send(sender, "次のマップ: " + id); }
                case "pos1", "pos2", "spectator", "recovery", "point" -> {
                    Double radius = operation.equals("point") && args.length > 2 ? Double.parseDouble(args[2]) : null;
                    maps.setPoint(id, operation, player(sender).getLocation(), radius);
                    if (operation.equals("point")) {
                        double savedRadius = maps.getDefinition(id).controlRadius();
                        send(sender, id + " の拠点を現在地に保存しました。半径 " + savedRadius + " ブロック / 直径 " + (savedRadius * 2) + " ブロック");
                    } else send(sender, id + " の " + operation + " を現在地に保存しました。");
                }
                case "radius" -> {
                    if (args.length != 3) throw new IllegalArgumentException("/otogi map radius <id> <半径1〜64>");
                    double radius = Double.parseDouble(args[2]);
                    maps.setControlRadius(id, radius);
                    send(sender, id + " の拠点半径を " + radius + " ブロック (直径 " + (radius * 2) + " ブロック) に保存しました。");
                }
                case "spawn" -> {
                    if (args.length < 3) throw new IllegalArgumentException("/otogi map spawn <id> <add|clear|list|remove [番号]>");
                    switch (args[2].toLowerCase(Locale.ROOT)) {
                        case "add" -> { maps.addSpawn(id, player(sender).getLocation()); send(sender, "現在地をspawnとして保存しました。"); }
                        case "clear" -> { maps.clearSpawns(id); send(sender, "spawn一覧を空にしました。"); }
                        case "remove" -> { if (args.length < 4) throw new IllegalArgumentException("削除するspawn番号が必要です。"); maps.removeSpawn(id, Integer.parseInt(args[3])); send(sender, "spawnを削除しました。"); }
                        case "list" -> {
                            MapDefinition map = maps.getDefinition(id);
                            if (map == null) throw new IllegalArgumentException("マップがありません: " + id);
                            int index = 1;
                            for (var point : map.spawns()) send(sender, index++ + ": " + point.x() + ", " + point.y() + ", " + point.z() + " / yaw " + point.yaw());
                        }
                        default -> throw new IllegalArgumentException("spawn操作: add | clear | list | remove <番号>");
                    }
                }
                default -> help(sender);
            }
        } catch (RuntimeException exception) {
            send(sender, Component.text(exception.getMessage() == null ? "設定を適用できませんでした。" : exception.getMessage(), NamedTextColor.RED));
        }
    }
    private static Player player(CommandSender sender) {
        if (sender instanceof Player player) return player;
        throw new IllegalArgumentException("現在地を指定する操作はゲーム内プレイヤー専用です。");
    }
    private static void help(CommandSender sender) {
        send(sender, "/otogi map list | info <id> | select <id> | reload\n設定: create <id> [名前] → pos1 <id> / pos2 <id> → spawn <id> add (2点以上)\n続けて spectator <id> / recovery <id> / point <id> [半径] → validate <id>\n拠点の位置を変えずに半径だけ変更: radius <id> <半径1〜64>\n全操作は /otogi map に続けて入力。設定は即JSON保存。地形は自分で用意してください。");
    }
    @Override public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> values = args.length == 1 ? List.of("list", "info", "create", "select", "pos1", "pos2", "spawn", "spectator", "recovery", "point", "radius", "validate", "reload")
                : args.length == 2 && !args[0].equalsIgnoreCase("create") ? matches.getMaps().getDefinitions().stream().map(MapDefinition::id).toList()
                : args.length == 3 && args[0].equalsIgnoreCase("spawn") ? List.of("add", "remove", "clear", "list")
                : args.length == 3 && (args[0].equalsIgnoreCase("radius") || args[0].equalsIgnoreCase("point")) ? List.of("1", "3", "5", "8", "10", "16") : List.of();
        String prefix = args.length == 0 ? "" : args[args.length-1].toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.startsWith(prefix)).toList();
    }
}
