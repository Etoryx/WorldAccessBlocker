package net.denfry.worldAccessBlocker.utils;

import net.denfry.worldAccessBlocker.WorldAccessBlocker;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class BypassManager {
    // Concurrent maps: on Folia these are read/written from multiple region threads.
    private final Map<UUID, Map<String, Instant>> bypasses = new ConcurrentHashMap<>();
    private final WorldAccessBlocker plugin;
    private final File bypassFile;
    private final AtomicBoolean saveScheduled = new AtomicBoolean(false);
    private volatile boolean dirty;

    public BypassManager(WorldAccessBlocker plugin) {
        this.plugin = plugin;
        this.bypassFile = new File(plugin.getDataFolder(), "bypasses.yml");
    }

    public void grantBypass(UUID playerId, String feature, Instant bypassUntil) {
        bypasses.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>()).put(feature, bypassUntil);
        scheduleSave();
    }

    public boolean removeBypass(UUID playerId, String feature) {
        Map<String, Instant> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.remove(feature) != null) {
            if (playerBypasses.isEmpty()) {
                bypasses.remove(playerId);
            }
            scheduleSave();
            return true;
        }
        return false;
    }

    /**
     * @return {@code true} when the player has no active bypass for the feature
     * (i.e. the global restriction still applies to them).
     */
    public boolean hasNoBypass(UUID playerId, String feature) {
        Map<String, Instant> playerBypasses = bypasses.get(playerId);
        if (playerBypasses != null && playerBypasses.containsKey(feature)) {
            Instant bypassUntil = playerBypasses.get(feature);
            if (bypassUntil != null && Instant.now().isBefore(bypassUntil)) {
                return false;
            } else {
                playerBypasses.remove(feature);
                if (playerBypasses.isEmpty()) {
                    bypasses.remove(playerId);
                }
                scheduleSave();
            }
        }
        return true;
    }

    public void loadBypasses() {
        bypasses.clear();
        if (!bypassFile.exists()) {
            return;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(bypassFile);
        for (String uuidStr : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection section = config.getConfigurationSection(uuidStr);
                if (section == null) continue;
                Map<String, Instant> playerBypasses = new ConcurrentHashMap<>();
                for (String feature : section.getKeys(false)) {
                    long epochSecond = section.getLong(feature);
                    playerBypasses.put(feature, Instant.ofEpochSecond(epochSecond));
                }
                if (!playerBypasses.isEmpty()) {
                    bypasses.put(uuid, playerBypasses);
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in bypasses.yml: " + uuidStr);
            }
        }
    }

    public void saveBypasses() {
        dirty = false;
        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, Instant>> entry : bypasses.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            for (Map.Entry<String, Instant> f : entry.getValue().entrySet()) {
                section.set(f.getKey(), f.getValue().getEpochSecond());
            }
        }
        try {
            config.save(bypassFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save bypasses: " + e.getMessage());
        }
    }

    /**
     * Debounced save: coalesces bursts of changes into a single disk write ~2s later,
     * dispatched through the platform runtime so it is safe on both Paper and Folia.
     */
    private void scheduleSave() {
        dirty = true;
        if (!saveScheduled.compareAndSet(false, true)) {
            return;
        }
        plugin.getRuntime().runLater(() -> {
            saveScheduled.set(false);
            if (dirty) {
                saveBypasses();
            }
        }, 40L);
    }

    public Instant getActiveBypassUntil(UUID playerId, String feature, Instant now) {
        Map<String, Instant> playerBypasses = bypasses.get(playerId);
        if (playerBypasses == null) return null;
        Instant bypassUntil = playerBypasses.get(feature);
        if (bypassUntil == null) return null;
        return now.isBefore(bypassUntil) ? bypassUntil : null;
    }
}
