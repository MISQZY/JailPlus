package org.misqzy.jailPlus;

import org.bukkit.plugin.java.JavaPlugin;
import org.misqzy.jailPlus.commands.JailAdminCommand;
import org.misqzy.jailPlus.commands.JailCommand;
import org.misqzy.jailPlus.commands.UnjailCommand;
import org.misqzy.jailPlus.listeners.PlayerListener;
import org.misqzy.jailPlus.managers.*;

import java.util.Objects;
import java.util.logging.Level;

public final class JailPlus extends JavaPlugin {

    private static JailPlus instance;

    // Core managers
    private ConfigManager configManager;
    private LocalizationManager localizationManager;
    private JailManager jailManager;

    // Advanced managers
    private PlaceholderManager placeholderManager;
    private StatisticsManager statisticsManager;
    private LogManager logManager;
    private ScoreboardManager scoreboardManager;

    private long startupTime;

    @Override
    public void onEnable() {
        startupTime = System.currentTimeMillis();
        instance = this;

        getLogger().fine("Author: " + getDescription().getAuthors().toString() + " Version: " + getDescription().getVersion());

        try {
            if (!initializeManagers()) {
                getLogger().severe("Critical error on managers initializing!");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }

            registerCommands();
            registerListeners();

            if (placeholderManager != null) {
                getServer().getScheduler().runTaskLater(this, () -> {
                    placeholderManager.reload();
                    logStartupInfo();
                }, 20L);
            } else {
                logStartupInfo();
            }

        } catch (Exception e) {
            getLogger().severe("Critical error while starting plugin: " + e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (jailManager != null) {
                jailManager.shutdown();
            }

            if (statisticsManager != null) {
                statisticsManager.shutdown();
            }

            if (placeholderManager != null) {
                placeholderManager.shutdown();
            }

            if (logManager != null) {
                logManager.shutdown();
            }

            if (scoreboardManager != null) {
                scoreboardManager.shutdown();
            }

            getLogger().info("JailPlus successfully disabled!");

        } catch (Exception e) {
            getLogger().severe("Error on plugin disabling: " + e);
        }
    }


    private boolean initializeManagers() {
        try {
            getLogger().fine("Manager initialization...");

            // Core managers
            configManager = new ConfigManager(this);
            localizationManager = new LocalizationManager(this, configManager);
            jailManager = new JailManager(this, configManager, localizationManager);

            // Advanced managers
            try {
                placeholderManager = new PlaceholderManager(this);
                getLogger().fine("PlaceholderManager initialized");
            } catch (Exception e) {
                getLogger().warning("Can't get access to PlaceholderManager: " + e.getMessage());
            }

            try {
                if (configManager.isStatisticsEnabled()) {
                    statisticsManager = new StatisticsManager(this);
                    getLogger().fine("StatisticsManager initialized");
                }
            } catch (Exception e) {
                getLogger().warning("Can't get access to StatisticsManager: " + e.getMessage());
            }

            try {
                if (configManager.isLoggingEnabled()) {
                    logManager = new LogManager(this);
                    getLogger().fine("LogManager initialized");
                }
            } catch (Exception e) {
                getLogger().warning("Can't get access to  LogManager: " + e.getMessage());
            }

            try {
                if (configManager.isScoreboardEnabled()) {
                    scoreboardManager = new ScoreboardManager(this);
                    getLogger().fine("ScoreboardManager initialized");
                }
            } catch (Exception e) {
                getLogger().warning("Can't get access to ScoreboardManager: " + e.getMessage());
            }

            getLogger().fine("All managers successfully initialized!");
            return true;

        } catch (Exception e) {
            getLogger().severe("Manager initialization error: " + e);
            return false;
        }
    }


    private void registerCommands() {
        try {
            Objects.requireNonNull(getCommand("jail"))
                    .setExecutor(new JailCommand(this, jailManager, localizationManager));
            Objects.requireNonNull(getCommand("unjail"))
                    .setExecutor(new UnjailCommand(this, jailManager, localizationManager));
            Objects.requireNonNull(getCommand("jailadmin"))
                    .setExecutor(new JailAdminCommand(this, jailManager, configManager, localizationManager));

            getLogger().fine("Commands registered");
        } catch (Exception e) {
            getLogger().severe("Command registration error: " +  e);
            throw e;
        }
    }


    private void registerListeners() {
        try {
            getServer().getPluginManager().registerEvents(
                    new PlayerListener(this, jailManager, localizationManager, configManager), this
            );
            getLogger().fine("Event handlers registered");
        } catch (Exception e) {
            getLogger().severe("Error registering handlers: " + e);
            throw e;
        }
    }


    public void reloadPlugin() {
        getLogger().info("JailPlus reload begins...");
        long reloadStart = System.currentTimeMillis();

        try {
            // Save current data
            if (jailManager != null) {
                jailManager.saveAllData();
            }

            if (scoreboardManager != null) {
                scoreboardManager.shutdown();
            }

            // Reload configurations
            configManager.reloadConfig();
            localizationManager.reloadMessages();
            jailManager.reloadData();

            // Reload advanced managers
            if (placeholderManager != null) {
                placeholderManager.reload();
            }

            if (statisticsManager != null) {
                statisticsManager.reload();
            }

            if (configManager.isScoreboardEnabled()) {
                scoreboardManager = new ScoreboardManager(this);
                getLogger().fine("ScoreboardManager recreated");
            } else {
                scoreboardManager = null;
                getLogger().fine("ScoreboardManager disabled");
            }

            long reloadTime = System.currentTimeMillis() - reloadStart;
            getLogger().info("JailPlus reloaded for " + reloadTime + "ms!");

        } catch (Exception e) {
            getLogger().severe("Error while reloading plugin: " + e);
            throw new RuntimeException("Reload error", e);
        }
    }


    private void logStartupInfo() {
        long startupDuration = System.currentTimeMillis() - startupTime;

        getLogger().fine("Jails loaded: " + jailManager.getAllJails().size());
        getLogger().fine("Prisoners: " + jailManager.getAllJailedPlayers().size());

        if (placeholderManager != null && placeholderManager.isPlaceholderAPIEnabled()) {
            getLogger().info("PlaceholderAPI: Active (" + placeholderManager.getHooks().size() + " hooks)");

            String[] placeholders = placeholderManager.getAvailablePlaceholders();
            getLogger().fine("Available placeholders: " + placeholders.length);

            if (configManager.isDebugEnabled()) {
                getLogger().fine("Placeholders list:");
                for (String placeholder : placeholders) {
                    getLogger().fine("  - " + placeholder);
                }
            }
        } else {
            getLogger().info("PlaceholderAPI: Unavailable");
        }

        if (statisticsManager != null) {
            getLogger().fine("Statistic: Enabled");
        }

        if (logManager != null) {
            getLogger().fine("Logging: Enabled");
        }

        if (scoreboardManager != null) {
            getLogger().fine("Scoreboard: Enabled (updating every " +
                    (configManager.getScoreboardUpdateInterval() / 20) + "s)");
        } else {
            getLogger().fine("Scoreboard: Disabled");
        }

        getLogger().info("JailPlus successfully enabled! (took " + startupDuration + "ms)");
    }


    public static JailPlus getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LocalizationManager getLocalizationManager() {
        return localizationManager;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
}
