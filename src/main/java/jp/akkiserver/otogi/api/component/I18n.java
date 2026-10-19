package jp.akkiserver.otogi.api.component;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.google.gson.JsonParser;
import jp.akkiserver.otogi.api.OtogiAPI;

public class I18n {
    private static final ServerTranslator translator = new ServerTranslator();

    public static void init() {
        commit(prepare());
        GlobalTranslator.translator().addSource(translator);
    }

    public static Map<Locale, Map<String, String>> prepare() {
        return prepare(OtogiAPI.plugin.getPlugin().getDataFolder().toPath().resolve("lang"));
    }

    public static Map<Locale, Map<String, String>> prepare(java.nio.file.Path directory) {
        Map<Locale, Map<String, String>> prepared = new HashMap<>();
        File folder = directory.toFile();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                Locale locale = Locale.forLanguageTag(file.getName().replace(".json", "").replace('_', '-'));
                try (var reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                    Map<String, String> entries = new HashMap<>();
                    JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
                        if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString())
                            throw new IllegalArgumentException("翻訳は文字列で指定してください: " + entry.getKey());
                        String text = entry.getValue().getAsString();
                        new MessageFormat(text, locale);
                        entries.put(entry.getKey(), text);
                    });
                    prepared.put(locale, Map.copyOf(entries));
                } catch (Exception exception) {
                    throw new IllegalArgumentException(file.getName() + " を読み込めません: " + exception.getMessage(), exception);
                }
            }
        }
        return Map.copyOf(prepared);
    }

    public static void commit(Map<Locale, Map<String, String>> prepared) { ServerTranslator.translations = Map.copyOf(prepared); }

    public static void shutdown() {
        GlobalTranslator.translator().removeSource(translator);
        ServerTranslator.translations = Map.of();
    }

    public static String immediately(String key, Locale locale) {
        var format = translator.translate(key, locale);
        return format != null ? format.format(null) : key;
    }

    public static String immediately(String key, Locale locale, @NotNull Object[] args) {
        var format = translator.translate(key, locale);
        return format != null ? format.format(args) : key;
    }

    @NotNullByDefault
    private static class ServerTranslator implements Translator {
        private static volatile Map<Locale, Map<String, String>> translations = Map.of();

        @Override
        public Key name() {
            return Key.key("otogi", "server");
        }

        @Override
        public @Nullable MessageFormat translate(String key, Locale locale) {
            var translation = translations.getOrDefault(locale, translations.get(Locale.JAPAN));
            return translation != null && translation.get(key) != null ? new MessageFormat(translation.get(key), locale) : null;
        }

        @Override
        public boolean canTranslate(String key, Locale locale) {
            var translation = translations.getOrDefault(locale, translations.get(Locale.JAPAN));
            return translation != null && translation.containsKey(key);
        }
    }
}
