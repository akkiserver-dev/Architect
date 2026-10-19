package jp.akkiserver.otogi.api.build;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jp.akkiserver.otogi.api.OtogiAPI;
import jp.akkiserver.otogi.api.architect.Architect;
import jp.akkiserver.otogi.api.architect.Park;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Holds the build of every player and saves them to {@code builds.yml}.
 */
public final class Builds {
    public static final int DEFAULT_SIZE = 5;
    public static final int MAX_SIZE = 9;

    private static final Map<UUID, Build> BUILDS = new HashMap<>();
    private static File file;

    /**
     * Gets how many architects a build can hold. Configured by {@code build.size}.
     */
    public static int getSize() {
        int size = OtogiAPI.plugin == null ? DEFAULT_SIZE : OtogiAPI.plugin.getConfig().getInt("build.size", DEFAULT_SIZE);
        return Math.clamp(size, 1, MAX_SIZE);
    }

    /**
     * Loads the saved builds from the file.
     */
    public static void load(@NotNull File target) {
        file = target;
        BUILDS.clear();
        if (!target.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(target);
        for (String key : yaml.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException exception) {
                continue;
            }
            Build build = new Build();
            for (String id : yaml.getStringList(key)) {
                NamespacedKey namespaced = NamespacedKey.fromString(id);
                Architect architect = namespaced == null ? null : OtogiAPI.getArchitect(namespaced);
                if (architect != null) {
                    add(build, architect);
                }
            }
            BUILDS.put(uuid, build);
        }
    }

    public static void save() {
        if (file == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        BUILDS.forEach((uuid, build) -> {
            if (!build.isEmpty()) {
                yaml.set(uuid.toString(), build.getIds().stream().map(NamespacedKey::toString).toList());
            }
        });
        Path temporary = null;
        try {
            Path target = file.toPath().toAbsolutePath();
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "builds-", ".tmp");
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            OtogiAPI.LOGGER.error("Unable to save builds", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException exception) {
                    OtogiAPI.LOGGER.warn("Unable to remove temporary build file {}", temporary, exception);
                }
            }
        }
    }

    /**
     * Gets the build of the player. A build is created on demand.
     */
    @NotNull
    public static Build get(@NotNull UUID player) {
        return BUILDS.computeIfAbsent(player, key -> new Build());
    }

    /** Reconcile removed definitions, changed kinds/incompatibilities and slot count after a successful reload. */
    public static void revalidateAll() {
        BUILDS.replaceAll((uuid, build) -> {
            var previous = build.getArchitects();
            build.clear();
            for (Architect architect : previous) add(build, architect);
            return build;
        });
        save();
    }

    /**
     * Checks whether the architect can be added to the build.
     * A second park is rejected.
     *
     * @return why it cannot be added, or {@code null} if it can
     */
    @Nullable
    public static String check(@NotNull Build build, @NotNull Architect candidate) {
        if (build.has(candidate)) {
            return "すでに選択されています。";
        }
        if (candidate instanceof Park && build.getPark() != null) {
            return "式は1つまでです。選択中の式を右クリックで外してください。";
        }
        if (build.size() >= getSize()) {
            return "選択枠が埋まっています（最大" + getSize() + "個）。右クリックで選択を外してください。";
        }
        for (Architect picked : build.getArchitects()) {
            if (picked instanceof Park park && park.isIncompatible(candidate)) {
                return "「" + plain(picked) + "」と併用できません。";
            }
            if (candidate instanceof Park park && park.isIncompatible(picked)) {
                return "「" + plain(picked) + "」と併用できません。";
            }
        }
        return null;
    }

    /**
     * Adds the architect if {@link #check} allows it.
     *
     * @return why it cannot be added, or {@code null} if it was added
     */
    @Nullable
    public static String add(@NotNull Build build, @NotNull Architect architect) {
        String reason = check(build, architect);
        if (reason != null) {
            return reason;
        }
        build.addUnchecked(architect);
        return null;
    }

    private static String plain(Architect architect) {
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(architect.getName());
    }

    /** Lists the architects in the order shown by menus: parks first, then by id. */
    @NotNull
    public static List<Architect> sorted(@NotNull java.util.Collection<Architect> architects) {
        return architects.stream()
                .sorted(java.util.Comparator.<Architect, Boolean>comparing(architect -> !(architect instanceof Park))
                        .thenComparing(architect -> architect.getClass().getPackageName())
                        .thenComparing(architect -> architect.getId().toString()))
                .toList();
    }

    private Builds() {}
}
