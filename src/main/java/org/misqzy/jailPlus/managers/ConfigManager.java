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

    public ConfigManager(JailPlus plugin) {
        this.plugin = plugin;
        setupConfig();
    }


    private void setupConfig() {
        plugin.saveDefaultConfig();

        configFile = new File(plugin.getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        setDefaults();

        saveConfig();
    }


    private void setDefaults() {
        config.addDefault("locale", DEFAULT_LANGUAGE);
        config.addDefault("use-prefix",true);
        config.addDefault("max-jail-time", DEFAULT_MAX_JAIL_TIME);
        config.addDefault("broadcast-jail", DEFAULT_BROADCAST_JAIL);
        config.addDefault("broadcast-unjail", true);
        config.addDefault("prevent-command-usage", true);

        config.addDefault("unblocked-commands", List.of(
                "say", "msg", "tell", "pm"
        ));

        config.options().copyDefaults(true);
    }


    public void reloadConfig() {
        try {
            config = YamlConfiguration.loadConfiguration(configFile);
            plugin.getLogger().info("Configuration reload successfully!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error when try reload configuration!", e);
        }
    }


    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error when try save configuration!", e);
        }
    }

    public String getLanguage() {
        return config.getString("locale", DEFAULT_LANGUAGE);
    }

    public boolean isUsePrefix() {
        return config.getBoolean("use-prefix", true);
    }

    public int getMaxJailTime() {
        return config.getInt("max-jail-time", DEFAULT_MAX_JAIL_TIME);
    }

    public boolean isBroadcastJail() {
        return config.getBoolean("broadcast-jail", DEFAULT_BROADCAST_JAIL);
    }

    public boolean isBroadcastUnjail() {
        return config.getBoolean("broadcast-unjail", true);
    }

    public boolean isPreventCommandUsage() {
        return config.getBoolean("prevent-command-usage", true);
    }

    public List<String> getBlockedCommands() {
        return config.getStringList("unblocked-commands");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
