package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LanguageManagerTest {

    @TempDir
    Path dataFolder;

    private void writeLang(String name, String content) throws IOException {
        File dir = new File(dataFolder.toFile(), "lang");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create lang dir");
        }
        Files.writeString(new File(dir, name).toPath(), content);
    }

    private WorldAccessBlocker pluginWithLanguage(String language) {
        WorldAccessBlocker plugin = mock(WorldAccessBlocker.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LanguageManagerTest"));
        // saveResource is a no-op mock, so our hand-written lang files are not overwritten.
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("language", language);
        when(plugin.getConfig()).thenReturn(cfg);
        return plugin;
    }

    @Test
    void missingKeyInCustomLanguageFallsBackToEnglish() throws IOException {
        writeLang("en.yml", "greeting: \"Hello\"\nfarewell: \"Bye\"\n");
        writeLang("de.yml", "greeting: \"Hallo\"\n"); // farewell intentionally omitted

        LanguageManager manager = new LanguageManager(pluginWithLanguage("de"));

        assertEquals("Hallo", manager.getMessage("greeting"));
        assertEquals("Bye", manager.getMessage("farewell"));
    }

    @Test
    void unknownLanguageStillServesEnglishKeys() throws IOException {
        writeLang("en.yml", "greeting: \"Hello\"\n");

        LanguageManager manager = new LanguageManager(pluginWithLanguage("xx"));

        assertEquals("Hello", manager.getMessage("greeting"));
    }
}
