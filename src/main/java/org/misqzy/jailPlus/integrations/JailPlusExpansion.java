package org.misqzy.jailPlus.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.hooks.PlaceholderHook;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.utils.TimeUtils;

import java.util.HashMap;
import java.util.Map;

public class JailPlusExpansion extends PlaceholderExpansion {

    private final JailPlus plugin;
    private final JailManager jailManager;
    private final Map<String, PlaceholderHook> customHooks;

    private final Map<String, String> cache;
    private long lastCacheClean = 0;

    public JailPlusExpansion(JailPlus plugin) {
        this.plugin = plugin;
        this.jailManager = plugin.getJailManager();
        this.customHooks = new HashMap<>();
        this.cache = new HashMap<>();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "jailplus";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        cleanCacheIfNeeded();


        String cacheKey = player.getUniqueId() + ":" + params;
        if (plugin.getConfigManager().isCachePlaceholders() && cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        String result = processPlaceholder(player, params);

        if (plugin.getConfigManager().isCachePlaceholders() && result != null) {
            cache.put(cacheKey, result);
        }

        return result != null ? result : "";
    }

    private String processPlaceholder(OfflinePlayer player, String params) {
        for (Map.Entry<String, PlaceholderHook> entry : customHooks.entrySet()) {
            if (params.startsWith(entry.getKey())) {
                String result = entry.getValue().onPlaceholderRequest(player, params);
                if (result != null) {
                    return result;
                }
            }
        }

        PlayerJailData jailData = jailManager.getJailData(player.getUniqueId());

        switch (params.toLowerCase()) {
            case "player_jailed":
                return jailData != null ? "true" : "false";

            case "player_jail_name":
                return jailData != null ? jailData.getJailName() : "";

            case "player_time_left":
                if (jailData != null) {
                    return TimeUtils.formatTime(jailData.getRemainingTime());
                }
                return "0s";

            case "player_time_left_seconds":
                return jailData != null ? String.valueOf(jailData.getRemainingTime()) : "0";

            case "player_reason":
                return jailData != null ? jailData.getReason() : "";

            case "player_jailed_by":
                return jailData != null ? jailData.getJailedBy() : "";

            case "jail_count":
                return String.valueOf(jailManager.getAllJails().size());

            case "prisoner_count":
                return String.valueOf(jailManager.getAllJailedPlayers().size());

            case "player_start_time":
                if (jailData != null) {
                    return String.valueOf(jailData.getStartTime());
                }
                return "0";

            case "player_jail_world":
                if (jailData != null) {
                    var jail = jailManager.getJail(jailData.getJailName());
                    return jail != null ? jail.getWorldName() : "";
                }
                return "";
        }


        if (params.startsWith("jail_prisoners_")) {
            String jailName = params.substring("jail_prisoners_".length());
            long count = jailManager.getAllJailedPlayers().stream()
                    .filter(data -> data.getJailName().equalsIgnoreCase(jailName))
                    .count();
            return String.valueOf(count);
        }

        if (params.startsWith("jail_exists_")) {
            String jailName = params.substring("jail_exists_".length());
            return jailManager.getJail(jailName) != null ? "true" : "false";
        }

        return null;
    }

    private void cleanCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCacheClean > plugin.getConfigManager().getPlaceholderCacheTime()) {
            cache.clear();
            lastCacheClean = now;
        }
    }

    public void registerHook(String prefix, PlaceholderHook hook) {
        customHooks.put(prefix.toLowerCase(), hook);
        plugin.getLogger().info("Custom placeholder hook registered: " + prefix);
    }

    public void unregisterHook(String prefix) {
        customHooks.remove(prefix.toLowerCase());
        plugin.getLogger().info("Custom placeholder hook deleted: " + prefix);
    }

    public Map<String, PlaceholderHook> getHooks() {
        return new HashMap<>(customHooks);
    }

    public void clearCache() {
        cache.clear();
    }
}