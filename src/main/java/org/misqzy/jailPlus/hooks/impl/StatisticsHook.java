package org.misqzy.jailPlus.hooks.impl;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.hooks.PlaceholderHook;
import org.misqzy.jailPlus.managers.StatisticsManager;


public class StatisticsHook implements PlaceholderHook {

    private final JailPlus plugin;
    private final StatisticsManager statisticsManager;

    public StatisticsHook(JailPlus plugin) {
        this.plugin = plugin;
        this.statisticsManager = plugin.getStatisticsManager();
    }

    @Override
    public @Nullable String onPlaceholderRequest(OfflinePlayer player, String params) {
        if (!params.startsWith("stats_")) {
            return null;
        }

        if (statisticsManager == null) {
            return "0"; // Fallback if statistics are disabled
        }

        String statParam = params.substring("stats_".length());

        switch (statParam.toLowerCase()) {
            case "total_jail_time":
                return String.valueOf(statisticsManager.getTotalJailTime(player.getUniqueId()));

            case "times_jailed":
                return String.valueOf(statisticsManager.getTimesJailed(player.getUniqueId()));

            case "worst_jail_time":
                return String.valueOf(statisticsManager.getLongestJailTime(player.getUniqueId()));

            case "is_frequent_offender":
                return statisticsManager.isFrequentOffender(player.getUniqueId()) ? "true" : "false";

            case "average_jail_time":
                return String.valueOf(statisticsManager.getAverageJailTime(player.getUniqueId()));

            case "last_jail_reason":
                return statisticsManager.getLastJailReason(player.getUniqueId());
        }

        return null;
    }

    @Override
    public String getHookPrefix() {
        return "stats";
    }

    @Override
    public String getDescription() {
        return "Statistic data about prisoned players ";
    }
}