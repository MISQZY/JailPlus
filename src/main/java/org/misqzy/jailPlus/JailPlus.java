package org.misqzy.jailPlus;

import org.bukkit.plugin.java.JavaPlugin;
import org.misqzy.jailPlus.commands.JailAdminCommand;
import org.misqzy.jailPlus.commands.JailCommand;
import org.misqzy.jailPlus.commands.UnjailCommand;
import org.misqzy.jailPlus.listeners.PlayerListener;
import org.misqzy.jailPlus.managers.ConfigManager;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;

import java.util.Objects;

public final class JailPlus extends JavaPlugin {

    private static JailPlus instance;

    private ConfigManager configManager;
    private LocalizationManager localizationManager;
    private JailManager jailManager;

    @Override
    public void onEnable() {
        instance = this;

        initializeManagers();

        registerCommands();

        registerListeners();

        getLogger().info("JailPlus v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        if (jailManager != null) {
            jailManager.saveAllData();
        }

        getLogger().info("JailPlus disabled!");
    }


    private void initializeManagers() {

        configManager = new ConfigManager(this);


        localizationManager = new LocalizationManager(this, configManager);


        jailManager = new JailManager(this, configManager, localizationManager);
    }


    private void registerCommands() {
        Objects.requireNonNull(getCommand("jail")).setExecutor(new JailCommand(this, jailManager, localizationManager));
        Objects.requireNonNull(getCommand("unjail")).setExecutor(new UnjailCommand(this,jailManager,localizationManager));
        Objects.requireNonNull(getCommand("jailadmin")).setExecutor(new JailAdminCommand(this, jailManager, configManager, localizationManager));
    }


    private void registerListeners() {
        getServer().getPluginManager().registerEvents(
                new PlayerListener(jailManager, localizationManager, configManager), this
        );
    }


    public void reloadPlugin() {
        getLogger().info("Reload configuration for JailPlus...");

        jailManager.saveAllData();

        configManager.reloadConfig();
        localizationManager.reloadMessages();
        jailManager.reloadData();

        getLogger().info("JailPlus configuration reloaded!");
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
}
