package org.misqzy.jailPlus.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.misqzy.jailPlus.JailPlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final JailPlus plugin;
    private FileConfiguration config;
    private File configFile;

    private static final String DEFAULT_LANGUAGE = "en";
    private static final int DEFAULT_MAX_JAIL_TIME = 86400;
    private static final boolean DEFAULT_BROADCAST_JAIL = true;
    private static final int DEFAULT_CHECK_INTERVAL = 600; // 30 seconds
    private static final boolean DEFAULT_ENABLE_STATISTICS = true;
    private static final boolean DEFAULT_ENABLE_LOGGING = true;
    private static final boolean DEFAULT_ENABLE_PLACEHOLDERAPI = true;

    public ConfigManager(JailPlus plugin) {
        this.plugin = plugin;
        setupConfig();
    }

    private void setupConfig() {
        plugin.saveDefaultConfig();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        if (isDebugEnabled())
            plugin.getLogger().setLevel(Level.FINE);
        else
            plugin.getLogger().setLevel(Level.INFO);

        setDefaults();
        saveConfig();
    }

    private void setDefaults() {
        // Basic settings
        config.addDefault("locale", DEFAULT_LANGUAGE);
        config.addDefault("use-prefix", true);
        config.addDefault("debug", false);

        // Jail settings
        config.addDefault("max-jail-time", DEFAULT_MAX_JAIL_TIME);
        config.addDefault("check-interval", DEFAULT_CHECK_INTERVAL);
        config.addDefault("auto-release", true);

        // Broadcast settings
        config.addDefault("broadcast-jail", DEFAULT_BROADCAST_JAIL);
        config.addDefault("broadcast-unjail", true);
        config.addDefault("notify-interval", 30);

        // Restrictions
        config.addDefault("prevent-command-usage", true);
        config.addDefault("prevent-block-break", true);
        config.addDefault("prevent-block-place", true);
        config.addDefault("prevent-pvp", true);
        config.addDefault("prevent-inventory", true);
        config.addDefault("prevent-teleport", true);
        config.addDefault("prevent-damage-to-prisoners", true);

        config.addDefault("unblocked-commands", List.of(
                "tell", "say", "msg", "server", "help", "rules"
        ));

        // Enhanced features
        config.addDefault("enable-logging", DEFAULT_ENABLE_LOGGING);
        config.addDefault("max-log-entries", 1000);
        config.addDefault("enable-statistics", DEFAULT_ENABLE_STATISTICS);
        config.addDefault("notify-admins", true);
        config.addDefault("admin-notification-permission", "jailplus.admin.notify");

        // Effects and sounds
        config.addDefault("enable-sounds", true);
        config.addDefault("jail-sound", "ENTITY_IRON_GOLEM_ATTACK");
        config.addDefault("unjail-sound", "ENTITY_PLAYER_LEVELUP");
        config.addDefault("enable-particles", true);
        config.addDefault("prisoner-particle", "SMOKE_NORMAL");

        // Database settings
        config.addDefault("database-type", "YAML");
        config.addDefault("mysql.host", "localhost");
        config.addDefault("mysql.port", 3306);
        config.addDefault("mysql.database", "jailplus");
        config.addDefault("mysql.username", "user");
        config.addDefault("mysql.password", "password");
        config.addDefault("mysql.table-prefix", "jp_");

        // PlaceholderAPI settings
        config.addDefault("enable-placeholderapi", DEFAULT_ENABLE_PLACEHOLDERAPI);
        config.addDefault("cache-placeholders", true);
        config.addDefault("placeholder-cache-time", 5000);

        config.options().copyDefaults(true);
    }

    public void reloadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("Configuration reloaded successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error when trying to reload configuration!", e);
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error when trying to save configuration!", e);
        }
    }

    // Basic getters
    public String getLanguage() {
        return config.getString("locale", DEFAULT_LANGUAGE);
    }

    public boolean isUsePrefix() {
        return config.getBoolean("use-prefix", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }

    // Jail settings
    public int getMaxJailTime() {
        return config.getInt("max-jail-time", DEFAULT_MAX_JAIL_TIME);
    }

    public int getCheckInterval() {
        return config.getInt("check-interval", DEFAULT_CHECK_INTERVAL);
    }

    public boolean isAutoRelease() {
        return config.getBoolean("auto-release", true);
    }

    // Broadcast settings
    public boolean isBroadcastJail() {
        return config.getBoolean("broadcast-jail", DEFAULT_BROADCAST_JAIL);
    }

    public boolean isBroadcastUnjail() {
        return config.getBoolean("broadcast-unjail", true);
    }

    public int getNotifyInterval() {
        return config.getInt("notify-interval", 30);
    }

    // Restrictions
    public boolean isPreventCommandUsage() {
        return config.getBoolean("prevent-command-usage", true);
    }

    public boolean isPreventBlockBreak() {
        return config.getBoolean("prevent-block-break", true);
    }

    public boolean isPreventBlockPlace() {
        return config.getBoolean("prevent-block-place", true);
    }

    public boolean isPreventPvP() {
        return config.getBoolean("prevent-pvp", true);
    }

    public boolean isPreventInventory() {
        return config.getBoolean("prevent-inventory", true);
    }

    public boolean isPreventTeleport() {
        return config.getBoolean("prevent-teleport", true);
    }

    public boolean isPreventDamageToPrisoners() {
        return config.getBoolean("prevent-damage-to-prisoners", true);
    }

    public List<String> getUnblockedCommands() {
        return config.getStringList("unblocked-commands");
    }

    // Enhanced features
    public boolean isLoggingEnabled() {
        return config.getBoolean("enable-logging", DEFAULT_ENABLE_LOGGING);
    }

    public int getMaxLogEntries() {
        return config.getInt("max-log-entries", 1000);
    }

    public boolean isStatisticsEnabled() {
        return config.getBoolean("enable-statistics", DEFAULT_ENABLE_STATISTICS);
    }

    public boolean isNotifyAdmins() {
        return config.getBoolean("notify-admins", true);
    }

    public String getAdminNotificationPermission() {
        return config.getString("admin-notification-permission", "jailplus.admin.notify");
    }

    // Effects and sounds
    public boolean isSoundsEnabled() {
        return config.getBoolean("enable-sounds", true);
    }

    public String getJailSound() {
        return config.getString("jail-sound", "ENTITY_IRON_GOLEM_ATTACK");
    }

    public String getUnjailSound() {
        return config.getString("unjail-sound", "ENTITY_PLAYER_LEVELUP");
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("enable-particles", true);
    }

    public String getPrisonerParticle() {
        return config.getString("prisoner-particle", "SMOKE_NORMAL");
    }

    // Database settings
    public String getDatabaseType() {
        return config.getString("database-type", "YAML");
    }

    public String getMySQLHost() {
        return config.getString("mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("mysql.database", "jailplus");
    }

    public String getMySQLUsername() {
        return config.getString("mysql.username", "user");
    }

    public String getMySQLPassword() {
        return config.getString("mysql.password", "password");
    }

    public String getMySQLTablePrefix() {
        return config.getString("mysql.table-prefix", "jp_");
    }

    // PlaceholderAPI settings
    public boolean isPlaceholderAPIEnabled() {
        return config.getBoolean("enable-placeholderapi", DEFAULT_ENABLE_PLACEHOLDERAPI);
    }

    public boolean isCachePlaceholders() {
        return config.getBoolean("cache-placeholders", true);
    }

    public long getPlaceholderCacheTime() {
        return config.getLong("placeholder-cache-time", 5000);
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
