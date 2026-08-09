package uk.akkiserver.otogi.api.component;

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

public class I18n {
    private static final ServerTranslator translator = new ServerTranslator();

    public static void init() {
        GlobalTranslator.translator().addSource(translator);
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
        private static final Map<Locale, Map<String, String>> translations = new HashMap<>();

        public static void registerTranslation(Locale locale, Map<String, String> translation) {
            translations.computeIfAbsent(locale, _ -> new HashMap<>()).putAll(translation);
        }

        @Override
        public Key name() {
            return Key.key("architect", "server");
        }

        @Override
        public @Nullable MessageFormat translate(String key, Locale locale) {
            var translation = translations.get(locale);
            return translation != null && translation.get(key) != null ? new MessageFormat(translation.get(key), locale) : null;
        }

        @Override
        public boolean canTranslate(String key, Locale locale) {
            var translation = translations.get(locale);
            return translation != null && translation.containsKey(key);
        }
    }
}
