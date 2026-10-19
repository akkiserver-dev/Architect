package jp.akkiserver.otogi.core.stage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import jp.akkiserver.otogi.api.stage.Stage;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads and writes only map metadata. Existing world terrain is never modified or generated. */
public final class MapService {
    private final Path directory;
    private final Map<String, MapDefinition> definitions = new LinkedHashMap<>();
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public MapService(JavaPlugin plugin) {
        directory = plugin.getDataFolder().toPath().resolve("maps");
        try {
            Files.createDirectories(directory);
            if (!Files.exists(directory.resolve("map.json.example"))) plugin.saveResource("maps/map.json.example", false);
        } catch (IOException exception) { throw new IllegalStateException("マップフォルダーを作成できません。", exception); }
        reload();
    }
    MapService(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
        reload();
    }
    /** All files are parsed before replacing definitions; an invalid file keeps the previous set. */
    public void reload() {
        commitReload(prepareReload());
    }
    public Map<String, MapDefinition> prepareReload() {
        try {
            Map<String, MapDefinition> loaded = new LinkedHashMap<>();
            try (var files = Files.list(directory)) {
                for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".json")).sorted().toList()) {
                    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        MapDefinition definition = JSON.fromJson(reader, MapDefinition.class);
                        if (definition == null || loaded.putIfAbsent(definition.id(), definition) != null)
                            throw new IllegalArgumentException("空または重複したマップです。");
                        if (!file.getFileName().toString().equals(definition.id() + ".json"))
                            throw new IllegalArgumentException("ファイル名はid.jsonにしてください。");
                    } catch (RuntimeException exception) {
                        throw new IllegalArgumentException("マップ " + file.getFileName() + ": " + exception.getMessage(), exception);
                    }
                }
            }
            return java.util.Collections.unmodifiableMap(loaded);
        } catch (IOException exception) { throw new IllegalStateException("マップJSONを読み込めません。", exception); }
    }
    public void commitReload(Map<String, MapDefinition> replacement) {
        definitions.clear();
        definitions.putAll(replacement);
    }
    public Collection<MapDefinition> getDefinitions() { return List.copyOf(definitions.values()); }
    public MapDefinition getDefinition(String id) { return definitions.get(id); }
    public String getDefaultMapId() { return definitions.keySet().stream().findFirst().orElse(null); }

    public void create(String id, String title, World world) {
        if (definitions.containsKey(id)) throw new IllegalArgumentException("同じIDが既にあります。");
        save(new MapDefinition(id, title, "ゲーム内で作成したマップ", world.getName(), world.getUID(), List.of(), null, null, null, 5, null, null));
    }
    public void setPoint(String id, String field, Location location, Double radius) {
        MapDefinition old = required(id);
        if (radius != null) validateControlRadius(radius);
        if (old.resolveWorld() != location.getWorld()) throw new IllegalArgumentException("登録したワールド内で設定してください: " + old.world());
        var point = MapDefinition.Point.of(location);
        save(new MapDefinition(old.id(), old.title(), old.description(), old.world(), old.worldId(), old.spawns(),
                field.equals("spectator") ? point : old.spectator(), field.equals("recovery") ? point : old.recovery(),
                field.equals("point") ? point : old.controlPoint(), radius == null ? old.controlRadius() : radius,
                field.equals("pos1") ? point : old.min(), field.equals("pos2") ? point : old.max()));
    }
    /** Changes the saved hill size without moving its center. */
    public void setControlRadius(String id, double radius) {
        MapDefinition old = required(id);
        validateControlRadius(radius);
        if (old.controlPoint() == null) throw new IllegalArgumentException("先に /otogi map point " + id + " で拠点を設定してください。");
        save(new MapDefinition(old.id(), old.title(), old.description(), old.world(), old.worldId(), old.spawns(),
                old.spectator(), old.recovery(), old.controlPoint(), radius, old.min(), old.max()));
    }
    private static void validateControlRadius(double radius) {
        if (!Double.isFinite(radius) || radius < 1 || radius > 64)
            throw new IllegalArgumentException("拠点半径は1〜64の有限な数値です。");
    }
    public void addSpawn(String id, Location location) {
        MapDefinition old = required(id);
        if (old.resolveWorld() != location.getWorld()) throw new IllegalArgumentException("登録したワールド内で設定してください。");
        List<MapDefinition.Point> spawns = new ArrayList<>(old.spawns());
        spawns.add(MapDefinition.Point.of(location));
        save(withSpawns(old, spawns));
    }
    public void clearSpawns(String id) { save(withSpawns(required(id), List.of())); }
    public void removeSpawn(String id, int index) {
        var old = required(id);
        var spawns = new ArrayList<>(old.spawns());
        if (index < 1 || index > spawns.size()) throw new IllegalArgumentException("spawn番号は1〜" + spawns.size() + "です。");
        spawns.remove(index - 1);
        save(withSpawns(old, spawns));
    }
    private MapDefinition withSpawns(MapDefinition old, List<MapDefinition.Point> spawns) {
        return new MapDefinition(old.id(), old.title(), old.description(), old.world(), old.worldId(), spawns, old.spectator(), old.recovery(), old.controlPoint(), old.controlRadius(), old.min(), old.max());
    }
    private MapDefinition required(String id) {
        MapDefinition definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("マップが見つかりません: " + id);
        return definition;
    }
    private void save(MapDefinition definition) {
        Path target = directory.resolve(definition.id() + ".json");
        Path temporary = directory.resolve(definition.id() + ".json.tmp");
        try {
            Files.writeString(temporary, JSON.toJson(definition), StandardCharsets.UTF_8);
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
            definitions.put(definition.id(), definition);
        } catch (IOException exception) { throw new IllegalStateException("マップを保存できません。", exception); }
    }

    /** Checks the existing loaded world and safe positions without changing a single block. */
    public List<String> getValidationIssues(String id) {
        MapDefinition map = required(id);
        List<String> issues = new ArrayList<>();
        if (map.resolveWorld() == null) issues.add("ワールド未ロード: " + map.world());
        if (map.min() == null) issues.add("pos1が未設定");
        if (map.max() == null) issues.add("pos2が未設定");
        if (map.spawns().size() < 2) issues.add("spawnを2点以上設定 (現在" + map.spawns().size() + "点)");
        if (map.spectator() == null) issues.add("spectatorが未設定");
        if (map.recovery() == null) issues.add("recoveryが未設定");
        if (!issues.isEmpty()) return List.copyOf(issues);
        try { prepare(id); } catch (IllegalArgumentException exception) { issues.add(exception.getMessage()); }
        return List.copyOf(issues);
    }
    public Stage prepare(String id) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("マップ検証はサーバースレッドで実行してください。");
        if (id == null) throw new IllegalArgumentException("マップ未登録です。/otogi map create <id> から設定してください。");
        MapDefinition definition = required(id);
        World world = definition.resolveWorld();
        if (world == null) throw new IllegalArgumentException("ワールドが未ロードです: " + definition.world());
        if (definition.spawns().size() < 2) throw new IllegalArgumentException("spawnを2点以上登録してください。");
        if (definition.min() == null || definition.max() == null) throw new IllegalArgumentException("pos1とpos2でプレイ範囲を設定してください。");
        if (definition.min().x() == definition.max().x() || definition.min().y() == definition.max().y() || definition.min().z() == definition.max().z())
            throw new IllegalArgumentException("プレイ範囲には幅・高さ・奥行きが必要です。");
        if (definition.spectator() == null || definition.recovery() == null) throw new IllegalArgumentException("spectatorとrecoveryを登録してください。");
        Stage stage = new ExistingWorldStage(definition, world);
        for (MapDefinition.Point point : definition.spawns()) validateSafe(world, point, stage, "spawn");
        validateSafe(world, definition.recovery(), stage, "recovery");
        validateSafe(world, definition.spectator(), stage, "spectator");
        if (definition.controlPoint() != null) {
            if (!stage.contains(definition.controlPoint().in(world).toLocation())) throw new IllegalArgumentException("拠点pointが範囲外です。");
            validateSafe(world, definition.controlPoint(), stage, "point");
        }
        return stage;
    }
    private static void validateSafe(World world, MapDefinition.Point point, Stage stage, String field) {
        Location location = point.in(world).toLocation();
        if (!stage.contains(location)) throw new IllegalArgumentException(field + "がプレイ範囲外です: " + point);
        if (point.y() <= world.getMinHeight() || point.y() + 1 >= world.getMaxHeight()) throw new IllegalArgumentException(field + "の高さがワールド範囲外です。");
        if (!world.isChunkGenerated(location.getBlockX() >> 4, location.getBlockZ() >> 4))
            throw new IllegalArgumentException(field + "は未生成チャンクです。既に作成した地形上で設定してください。");
        var feet = world.getBlockAt(location);
        var below = feet.getRelative(0, -1, 0);
        if (!feet.isPassable() || !feet.getRelative(0, 1, 0).isPassable() || !below.getType().isSolid()
                || dangerous(feet.getType()) || dangerous(feet.getRelative(0, 1, 0).getType()) || dangerous(below.getType()))
            throw new IllegalArgumentException(field + "の足場または頭上が安全ではありません: " + point);
    }
    private static boolean dangerous(Material type) {
        return switch (type) {
            case LAVA, FIRE, SOUL_FIRE, MAGMA_BLOCK, CAMPFIRE, SOUL_CAMPFIRE, CACTUS, SWEET_BERRY_BUSH, POWDER_SNOW, WITHER_ROSE -> true;
            default -> false;
        };
    }
}
