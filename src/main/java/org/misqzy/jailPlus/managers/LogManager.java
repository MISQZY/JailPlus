package org.misqzy.jailPlus.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.misqzy.jailPlus.JailPlus;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;


public class LogManager {

    private final JailPlus plugin;
    private final File logsFile;
    private FileConfiguration logs;
    private final SimpleDateFormat dateFormat;
    private final List<LogEntry> logCache;

    public LogManager(JailPlus plugin) {
        this.plugin = plugin;
        this.logCache = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Setup logs file
        logsFile = new File(plugin.getDataFolder(), "logs.yml");
        if (!logsFile.exists()) {
            try {
                logsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create logs.yml: " + e);
            }
        }

        logs = YamlConfiguration.loadConfiguration(logsFile);
        loadLogs();
    }

    private void loadLogs() {
        List<String> logEntries = logs.getStringList("logs");
        logCache.clear();

        for (String entry : logEntries) {
            String[] parts = entry.split("\\|", 5);
            if (parts.length >= 5) {
                LogEntry logEntry = new LogEntry(
                        parts[0], // timestamp
                        parts[1], // action
                        parts[2], // player
                        parts[3], // executor
                        parts[4]  // details
                );
                logCache.add(logEntry);
            }
        }

        plugin.getLogger().fine("Loaded " + logCache.size() + " log entries");
    }

    public void logJail(String playerName, String executor, String jailName, long time, String reason) {
        String details = String.format("Jail: %s, Time: %ds, Reason: %s", jailName, time, reason);
        addLog("JAIL", playerName, executor, details);
    }

    public void logUnjail(String playerName, String executor, String reason) {
        String details = String.format("Reason: %s", reason);
        addLog("UNJAIL", playerName, executor, details);
    }

    public void logTimeChange(String playerName, String executor, String action, long time) {
        String details = String.format("Action: %s, Time: %ds", action, time);
        addLog("TIME_CHANGE", playerName, executor, details);
    }

    public void logJailCreate(String executor, String jailName, String world, double x, double y, double z) {
        String details = String.format("Jail: %s, Location: %s %.1f %.1f %.1f", jailName, world, x, y, z);
        addLog("JAIL_CREATE", "-", executor, details);
    }

    public void logJailDelete(String executor, String jailName) {
        String details = String.format("Jail: %s", jailName);
        addLog("JAIL_DELETE", "-", executor, details);
    }

    private void addLog(String action, String player, String executor, String details) {
        String timestamp = dateFormat.format(new Date());
        LogEntry entry = new LogEntry(timestamp, action, player, executor, details);

        logCache.add(entry);

        // Trim logs if exceeding max entries
        int maxEntries = plugin.getConfigManager().getMaxLogEntries();
        if (maxEntries > 0 && logCache.size() > maxEntries) {
            logCache.remove(0); // Remove oldest entry
        }

        saveLogs();

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(String.format("[LOG] %s | %s | %s | %s | %s",
                    timestamp, action, player, executor, details));
        }
    }

    public List<LogEntry> getLogs() {
        return new ArrayList<>(logCache);
    }

    public List<LogEntry> getLogsForPlayer(String playerName) {
        return logCache.stream()
                .filter(entry -> entry.getPlayer().equals(playerName))
                .toList();
    }

    public List<LogEntry> getLogsByAction(String action) {
        return logCache.stream()
                .filter(entry -> entry.getAction().equals(action))
                .toList();
    }

    private void saveLogs() {
        List<String> logEntries = new ArrayList<>();

        for (LogEntry entry : logCache) {
            String logLine = String.format("%s|%s|%s|%s|%s",
                    entry.getTimestamp(),
                    entry.getAction(),
                    entry.getPlayer(),
                    entry.getExecutor(),
                    entry.getDetails()
            );
            logEntries.add(logLine);
        }

        logs.set("logs", logEntries);

        try {
            logs.save(logsFile);
        } catch (IOException e) {
            plugin.getLogger().severe( "Could not save logs.yml: " + e);
        }
    }

    public void reload() {
        logs = YamlConfiguration.loadConfiguration(logsFile);
        loadLogs();
    }

    public void shutdown() {
        saveLogs();
        logCache.clear();
    }

    public static class LogEntry {
        private final String timestamp;
        private final String action;
        private final String player;
        private final String executor;
        private final String details;

        public LogEntry(String timestamp, String action, String player, String executor, String details) {
            this.timestamp = timestamp;
            this.action = action;
            this.player = player;
            this.executor = executor;
            this.details = details;
        }

        public String getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public String getPlayer() { return player; }
        public String getExecutor() { return executor; }
        public String getDetails() { return details; }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s by %s - %s", timestamp, action, player, executor, details);
        }
    }
}