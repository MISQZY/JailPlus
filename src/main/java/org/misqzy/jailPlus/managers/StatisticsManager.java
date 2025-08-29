package org.misqzy.jailPlus.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.misqzy.jailPlus.JailPlus;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatisticsManager {

    private final JailPlus plugin;
    private final File statisticsFile;
    private FileConfiguration statistics;
    private final Map<UUID, PlayerStatistics> cache;

    public StatisticsManager(JailPlus plugin) {
        this.plugin = plugin;
        this.cache = new HashMap<>();

        statisticsFile = new File(plugin.getDataFolder(), "statistics.yml");
        if (!statisticsFile.exists()) {
            try {
                statisticsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create statistics.yml", e);
            }
        }

        statistics = YamlConfiguration.loadConfiguration(statisticsFile);
        loadStatistics();
    }

    private void loadStatistics() {
        if (statistics.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidString : statistics.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                String path = "players." + uuidString;

                PlayerStatistics stats = new PlayerStatistics();
                stats.totalJailTime = statistics.getLong(path + ".total-jail-time", 0);
                stats.timesJailed = statistics.getInt(path + ".times-jailed", 0);
                stats.longestJailTime = statistics.getLong(path + ".longest-jail-time", 0);
                stats.lastJailReason = statistics.getString(path + ".last-jail-reason", "");

                cache.put(uuid, stats);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in statistics: " + uuidString);
            }
        }

        plugin.getLogger().info("Loaded statistics for " + cache.size() + " players");
    }

    public void addJailRecord(UUID playerUuid, long jailTime, String reason) {
        PlayerStatistics stats = cache.computeIfAbsent(playerUuid, k -> new PlayerStatistics());

        stats.totalJailTime += jailTime;
        stats.timesJailed++;
        stats.lastJailReason = reason;

        if (jailTime > stats.longestJailTime) {
            stats.longestJailTime = jailTime;
        }

        savePlayerStatistics(playerUuid, stats);
    }

    public long getTotalJailTime(UUID playerUuid) {
        return cache.getOrDefault(playerUuid, new PlayerStatistics()).totalJailTime;
    }

    public int getTimesJailed(UUID playerUuid) {
        return cache.getOrDefault(playerUuid, new PlayerStatistics()).timesJailed;
    }

    public long getLongestJailTime(UUID playerUuid) {
        return cache.getOrDefault(playerUuid, new PlayerStatistics()).longestJailTime;
    }

    public String getLastJailReason(UUID playerUuid) {
        return cache.getOrDefault(playerUuid, new PlayerStatistics()).lastJailReason;
    }

    public long getAverageJailTime(UUID playerUuid) {
        PlayerStatistics stats = cache.get(playerUuid);
        if (stats == null || stats.timesJailed == 0) {
            return 0;
        }
        return stats.totalJailTime / stats.timesJailed;
    }

    public boolean isFrequentOffender(UUID playerUuid) {
        return getTimesJailed(playerUuid) >= 5;
    }

    private void savePlayerStatistics(UUID playerUuid, PlayerStatistics stats) {
        String path = "players." + playerUuid.toString();
        statistics.set(path + ".total-jail-time", stats.totalJailTime);
        statistics.set(path + ".times-jailed", stats.timesJailed);
        statistics.set(path + ".longest-jail-time", stats.longestJailTime);
        statistics.set(path + ".last-jail-reason", stats.lastJailReason);

        saveStatistics();
    }

    public void saveStatistics() {
        try {
            statistics.save(statisticsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save statistics.yml", e);
        }
    }

    public void reload() {
        cache.clear();
        statistics = YamlConfiguration.loadConfiguration(statisticsFile);
        loadStatistics();
    }

    public void shutdown() {
        saveStatistics();
        cache.clear();
    }

    private static class PlayerStatistics {
        long totalJailTime = 0;
        int timesJailed = 0;
        long longestJailTime = 0;
        String lastJailReason = "";
    }
}