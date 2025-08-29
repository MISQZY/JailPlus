package org.misqzy.jailPlus.managers;

import org.bukkit.Bukkit;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.hooks.PlaceholderHook;
import org.misqzy.jailPlus.hooks.impl.StatisticsHook;
import org.misqzy.jailPlus.integrations.JailPlusExpansion;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


public class PlaceholderManager {

    private final JailPlus plugin;
    private JailPlusExpansion expansion;
    private final Map<String, PlaceholderHook> hooks;
    private boolean placeholderAPIEnabled = false;

    public PlaceholderManager(JailPlus plugin) {
        this.plugin = plugin;
        this.hooks = new HashMap<>();
        initializePlaceholderAPI();
        registerDefaultHooks();
    }

    private void initializePlaceholderAPI() {
        if (!plugin.getConfigManager().isPlaceholderAPIEnabled()) {
            plugin.getLogger().info("PlaceholderAPI integration disabled in config file.");
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            plugin.getLogger().info("PlaceholderAPI not found. Placeholders unavailable.");
            return;
        }

        try {
            expansion = new JailPlusExpansion(plugin);

            if (expansion.register()) {
                placeholderAPIEnabled = true;
                plugin.getLogger().info("PlaceholderAPI integration successfully activated!");
                plugin.getLogger().info("Available placeholders: %jailplus_<placeholder>%");
            } else {
                plugin.getLogger().warning("Can't register PlaceholderAPI extensions!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe( "Error on PlaceholderAPI initialization: " + e);
        }
    }

    private void registerDefaultHooks() {
        if (!isPlaceholderAPIEnabled()) {
            return;
        }

        // Register default org.misqzy.jailPlus.hooks
        if (plugin.getStatisticsManager() != null) {
            registerHook(new StatisticsHook(plugin));
        }

        plugin.getLogger().fine("Registered defaults placeholders: " + hooks.size());
    }

    public boolean registerHook(PlaceholderHook hook) {
        if (!isPlaceholderAPIEnabled()) {
            plugin.getLogger().warning("PlaceholderAPI unavailable. Hook can't be registered: " + hook.getHookPrefix());
            return false;
        }

        if (hooks.containsKey(hook.getHookPrefix().toLowerCase())) {
            plugin.getLogger().warning("Hook with prefix '" + hook.getHookPrefix() + "' already registered!");
            return false;
        }

        hooks.put(hook.getHookPrefix().toLowerCase(), hook);
        expansion.registerHook(hook.getHookPrefix(), hook);

        plugin.getLogger().fine("Placeholder registered: " + hook.getHookPrefix() + " (" + hook.getDescription() + ")");
        return true;
    }

    public boolean unregisterHook(String prefix) {
        if (!isPlaceholderAPIEnabled()) {
            return false;
        }

        PlaceholderHook hook = hooks.remove(prefix.toLowerCase());
        if (hook != null) {
            expansion.unregisterHook(prefix);
            plugin.getLogger().fine("Placeholder deleted: " + prefix);
            return true;
        }

        return false;
    }

    public Map<String, PlaceholderHook> getHooks() {
        return new HashMap<>(hooks);
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public void reload() {
        if (expansion != null) {
            expansion.unregister();
        }

        hooks.clear();
        initializePlaceholderAPI();
        registerDefaultHooks();

        plugin.getLogger().fine("Placeholder system reloaded!");
    }

    public void shutdown() {
        if (expansion != null) {
            expansion.unregister();
        }
        hooks.clear();
        placeholderAPIEnabled = false;
    }

    public String[] getAvailablePlaceholders() {
        return new String[]{
                "%jailplus_player_jailed%",
                "%jailplus_player_jail_name%",
                "%jailplus_player_time_left%",
                "%jailplus_player_time_left_seconds%",
                "%jailplus_player_reason%",
                "%jailplus_player_jailed_by%",
                "%jailplus_jail_count%",
                "%jailplus_prisoner_count%",
                "%jailplus_player_start_time%",
                "%jailplus_player_jail_world%",
                "%jailplus_jail_prisoners_<jailname>%",
                "%jailplus_jail_exists_<jailname>%",
                // Custom org.misqzy.jailPlus.hooks
                "%jailplus_stats_total_jail_time%",
                "%jailplus_stats_times_jailed%",
                "%jailplus_stats_worst_jail_time%",
                "%jailplus_stats_is_frequent_offender%",
                "%jailplus_stats_average_jail_time%",
                "%jailplus_stats_last_jail_reason%"
        };
    }

    public void clearCache() {
        if (expansion != null) {
            expansion.clearCache();
        }
    }
}