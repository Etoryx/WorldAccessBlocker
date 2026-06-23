package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final WorldAccessBlocker plugin;
    private final Map<String, String> messages = new HashMap<>();
    private String language;

    public LanguageManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        reloadLanguage();
    }

    public void reloadLanguage() {
        this.language = plugin.getConfig().getString("language", "en");
        messages.clear();
        loadLanguageFile();
    }

    private void loadLanguageFile() {
        plugin.saveResource("lang/en.yml", false);
        plugin.saveResource("lang/ru.yml", false);

        // Always load English first as a base layer so missing keys in other
        // languages fall back to English instead of showing "Message not found".
        File enFile = new File(plugin.getDataFolder(), "lang/en.yml");
        putAll(YamlConfiguration.loadConfiguration(enFile));

        if (!"en".equalsIgnoreCase(language)) {
            File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
            if (langFile.exists()) {
                putAll(YamlConfiguration.loadConfiguration(langFile));
            } else {
                plugin.getLogger().warning("Language file lang/" + language + ".yml not found, falling back to en.yml");
            }
        }
    }

    private void putAll(FileConfiguration langConfig) {
        for (String key : langConfig.getKeys(true)) {
            if (!langConfig.isConfigurationSection(key)) {
                messages.put(key, langConfig.getString(key, ""));
            }
        }
    }

    public String getMessage(String key, Object... args) {
        String message = messages.getOrDefault(key, "Message not found: " + key);
        if (args.length > 0) {
            if (args[0] instanceof Instant restrictionInstant) {
                Duration durationLeft = Duration.between(Instant.now(), restrictionInstant);
                if (!durationLeft.isNegative()) {
                    long totalSeconds = durationLeft.getSeconds();
                    long days = totalSeconds / (24 * 3600);
                    long hours = (totalSeconds % (24 * 3600)) / 3600;
                    return String.format(message, days, hours);
                } else {
                    return messages.getOrDefault(key + "_expired", "Restriction has expired.");
                }
            } else if (args.length > 1 && args[0] instanceof String placeholder && args[1] instanceof Instant restrictionInstant) {
                Duration durationLeft = Duration.between(Instant.now(), restrictionInstant);
                if (!durationLeft.isNegative()) {
                    long totalSeconds = durationLeft.getSeconds();
                    long days = totalSeconds / (24 * 3600);
                    long hours = (totalSeconds % (24 * 3600)) / 3600;
                    return String.format(message, placeholder, days, hours);
                } else {
                    return String.format(messages.getOrDefault(key + "_expired", "Restriction for %s has expired."), placeholder);
                }
            }
        }
        return String.format(message, args);
    }
}