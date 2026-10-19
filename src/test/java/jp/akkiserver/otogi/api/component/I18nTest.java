package jp.akkiserver.otogi.api.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;

class I18nTest {
    @TempDir Path directory;
    @Test void translationsAreValidatedBeforePublicationAndInvalidFilesKeepPreviousValues() throws Exception {
        Path file = directory.resolve("ja_jp.json");
        Files.writeString(file, "{\"test.translation\":\"現在の値\"}");
        I18n.commit(I18n.prepare(directory));
        Files.writeString(file, "{\"test.translation\":\"変更後\"}");
        var candidate = I18n.prepare(directory);
        assertEquals("現在の値", I18n.immediately("test.translation", Locale.JAPAN));
        Files.writeString(file, "{\"test.translation\":\"{ invalid\"}");
        assertThrows(IllegalArgumentException.class, () -> I18n.prepare(directory));
        assertEquals("現在の値", I18n.immediately("test.translation", Locale.JAPAN));
        I18n.commit(candidate);
        assertEquals("変更後", I18n.immediately("test.translation", Locale.JAPAN));
        I18n.shutdown();
    }
}
