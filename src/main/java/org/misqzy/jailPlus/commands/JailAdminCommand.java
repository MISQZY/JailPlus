package org.misqzy.jailPlus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.JailData;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.managers.ConfigManager;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;
import org.misqzy.jailPlus.managers.LogManager;
import org.misqzy.jailPlus.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JailAdminCommand implements CommandExecutor, TabCompleter {

    private final JailPlus plugin;
    private final JailManager jailManager;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;

    public JailAdminCommand(JailPlus plugin, JailManager jailManager, ConfigManager configManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("jailplus.admin")) {
            localizationManager.sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreateJail(sender, args);
                break;

            case "delete":
            case "remove":
                handleDeleteJail(sender, args);
                break;

            case "time":
                handleTimeCommand(sender, args);
                break;

            case "list":
                handleListJails(sender);
                break;

            case "info":
                handleJailInfo(sender, args);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "logs":
                handleLogs(sender, args);
                break;

            case "stats":
                handleStats(sender, args);
                break;

            case "placeholders":
                handlePlaceholders(sender);
                break;

            case "help":
                showHelp(sender);
                break;

            default:
                localizationManager.sendMessage(sender, "admin.unknown-command", subCommand);
                showHelp(sender);
                break;
        }

        return true;
    }

    private void handleCreateJail(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            localizationManager.sendMessage(sender, "admin.player-only");
            return;
        }

        if (!sender.hasPermission("jailplus.admin.create")) {
            localizationManager.sendMessage(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            localizationManager.sendMessage(player, "admin.create-usage");
            return;
        }

        String jailName = args[1].toLowerCase();

        if (jailManager.getJail(jailName) != null) {
            localizationManager.sendMessage(player, "admin.jail-exists", jailName);
            return;
        }

        boolean success = jailManager.createJail(jailName, player.getLocation());
        if (success) {
            localizationManager.sendMessage(player, "admin.jail-created", jailName);
        } else {
            localizationManager.sendMessage(player, "admin.jail-create-failed", jailName);
        }
    }

    private void handleDeleteJail(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.delete"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            localizationManager.sendMessage(sender, "admin.delete-usage");
            return;
        }

        String jailName = args[1].toLowerCase();

        if (jailManager.getJail(jailName) == null) {
            localizationManager.sendMessage(sender, "admin.jail-not-exists", jailName);
            return;
        }

        boolean success = jailManager.deleteJail(jailName);
        if (success) {
            localizationManager.sendMessage(sender, "admin.jail-deleted", jailName);
        } else {
            localizationManager.sendMessage(sender, "admin.jail-delete-failed", jailName);
        }
    }

    private void handleListJails(CommandSender sender) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.list"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (jailManager.getAllJails().isEmpty()) {
            localizationManager.sendMessage(sender, "admin.no-jails");
            return;
        }

        localizationManager.sendMessage(sender, "admin.jails-header");

        for (JailData jail : jailManager.getAllJails()) {
            long prisonersCount = jailManager.getAllJailedPlayers().stream()
                    .filter(data -> data.getJailName().equals(jail.getName()))
                    .count();

            localizationManager.sendMessage(sender, "admin.jail-entry",
                    jail.getName(),
                    jail.getWorldName(),
                    (int) jail.getX(),
                    (int) jail.getY(),
                    (int) jail.getZ(),
                    prisonersCount
            );
        }
    }

    private void handleJailInfo(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.info"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            localizationManager.sendMessage(sender, "admin.info-usage");
            return;
        }

        String jailName = args[1].toLowerCase();
        JailData jail = jailManager.getJail(jailName);

        if (jail == null) {
            localizationManager.sendMessage(sender, "admin.jail-not-exists", jailName);
            return;
        }

        long prisonersCount = jailManager.getAllJailedPlayers().stream()
                .filter(data -> data.getJailName().equals(jailName))
                .count();

        localizationManager.sendMessage(sender, "admin.jail-info",
                jail.getName(),
                jail.getWorldName(),
                jail.getX(),
                jail.getY(),
                jail.getZ(),
                prisonersCount
        );
    }

    private void handleTimeCommand(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.time"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 4) {
            localizationManager.sendMessage(sender, "jail.time-usage");
            return;
        }

        String targetName = args[1];
        String action = args[2].toLowerCase();
        String timeString = args[3];

        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            localizationManager.sendMessage(sender, "player.not-found", targetName);
            return;
        }

        PlayerJailData jailData = jailManager.getJailData(target);
        if (jailData == null) {
            localizationManager.sendMessage(sender, "jail.not-jailed", target.getName());
            return;
        }

        try {
            long time = TimeUtils.parseTime(timeString);

            String executorName = sender instanceof Player ? sender.getName() : "Console";

            switch (action) {
                case "add":
                    jailData.addTime(time);
                    localizationManager.sendMessage(sender, "jail.time-added",
                            target.getName(), TimeUtils.formatTime(time));

                    if (plugin.getLogManager() != null) {
                        plugin.getLogManager().logTimeChange(target.getName(), executorName, "ADD", time);
                    }
                    break;

                case "remove":
                case "subtract":
                    jailData.subtractTime(time);
                    localizationManager.sendMessage(sender, "jail.time-removed",
                            target.getName(), TimeUtils.formatTime(time));

                    if (plugin.getLogManager() != null) {
                        plugin.getLogManager().logTimeChange(target.getName(), executorName, "SUBTRACT", time);
                    }
                    break;

                case "set":
                    jailData.setTime(time);
                    localizationManager.sendMessage(sender, "jail.time-set",
                            target.getName(), TimeUtils.formatTime(time));

                    if (plugin.getLogManager() != null) {
                        plugin.getLogManager().logTimeChange(target.getName(), executorName, "SET", time);
                    }
                    break;

                default:
                    localizationManager.sendMessage(sender, "jail.time-usage");
                    return;
            }

            jailManager.savePlayers();

        } catch (IllegalArgumentException e) {
            localizationManager.sendMessage(sender, "jail.invalid-time-format");
        }
    }

    private void handleReload(CommandSender sender) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.reload"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        long startTime = System.currentTimeMillis();

        try {
            plugin.reloadPlugin();
            long duration = System.currentTimeMillis() - startTime;

            localizationManager.sendMessage(sender, "admin.reload-success", duration);
        } catch (Exception e) {
            localizationManager.sendMessage(sender, "admin.reload-failed", e.getMessage());
            plugin.getLogger().severe("Error when reloading plugin: " + e.getMessage());
        }
    }

    private void handleLogs(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.logs"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        LogManager logManager = plugin.getLogManager();
        if (logManager == null) {
            localizationManager.sendMessage(sender, "admin.logs-disabled");
            return;
        }

        List<LogManager.LogEntry> logs;

        if (args.length > 1) {
            String filter = args[1].toLowerCase();
            switch (filter) {
                case "jail":
                    logs = logManager.getLogsByAction("JAIL");
                    break;
                case "unjail":
                    logs = logManager.getLogsByAction("UNJAIL");
                    break;
                default:
                    logs = logManager.getLogsForPlayer(args[1]);
                    break;
            }
        } else {
            logs = logManager.getLogs();
        }

        if (logs.isEmpty()) {
            localizationManager.sendMessage(sender, "admin.no-logs");
            return;
        }

        localizationManager.sendMessage(sender, "admin.logs-header");

        // Show last 10 entries
        int start = Math.max(0, logs.size() - 10);
        for (int i = start; i < logs.size(); i++) {
            LogManager.LogEntry entry = logs.get(i);
            sender.sendMessage("§7[" + entry.getTimestamp() + "] §e" + entry.getAction() +
                    " §f" + entry.getPlayer() + " §7by §f" + entry.getExecutor() +
                    " §7- §f" + entry.getDetails());
        }
    }

    private void handleStats(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.admin.stats"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (plugin.getStatisticsManager() == null) {
            localizationManager.sendMessage(sender, "admin.stats-disabled");
            return;
        }

        localizationManager.sendMessage(sender, "admin.stats-header");
        localizationManager.sendMessage(sender, "admin.stats-general",
                jailManager.getAllJails().size(),
                jailManager.getAllJailedPlayers().size()
        );

        if (args.length > 1) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                var statsManager = plugin.getStatisticsManager();
                sender.sendMessage("§eStats for " + target.getName() + ":");
                sender.sendMessage("§7Times jailed: §f" + statsManager.getTimesJailed(target.getUniqueId()));
                sender.sendMessage("§7Total jail time: §f" + TimeUtils.formatTime(statsManager.getTotalJailTime(target.getUniqueId())));
                sender.sendMessage("§7Longest jail: §f" + TimeUtils.formatTime(statsManager.getLongestJailTime(target.getUniqueId())));
                sender.sendMessage("§7Frequent offender: §f" + (statsManager.isFrequentOffender(target.getUniqueId()) ? "Yes" : "No"));
            }
        }
    }

    private void handlePlaceholders(CommandSender sender) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.placeholders"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (plugin.getPlaceholderManager() == null || !plugin.getPlaceholderManager().isPlaceholderAPIEnabled()) {
            localizationManager.sendMessage(sender, "admin.placeholders-disabled");
            return;
        }

        localizationManager.sendMessage(sender, "admin.placeholders-header");

        String[] placeholders = plugin.getPlaceholderManager().getAvailablePlaceholders();
        for (String placeholder : placeholders) {
            sender.sendMessage("§7- §e" + placeholder);
        }

        sender.sendMessage("§aTotal: " + placeholders.length + " placeholders available");
    }

    private void showHelp(CommandSender sender) {
        localizationManager.sendMessage(sender, "admin.help-header");
        localizationManager.sendMessage(sender, "admin.help-create");
        localizationManager.sendMessage(sender, "admin.help-delete");
        localizationManager.sendMessage(sender, "admin.help-list");
        localizationManager.sendMessage(sender, "admin.help-info");
        localizationManager.sendMessage(sender, "admin.help-reload");
        localizationManager.sendMessage(sender, "admin.help-time");
        localizationManager.sendMessage(sender, "admin.help-logs");
        localizationManager.sendMessage(sender, "admin.help-stats");
        localizationManager.sendMessage(sender, "admin.help-placeholders");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list", "info", "reload",
                    "help", "time", "logs", "stats", "placeholders"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("info")) {
                jailManager.getAllJails().forEach(jail -> completions.add(jail.getName()));
            } else if (args[0].equalsIgnoreCase("time") || args[0].equalsIgnoreCase("stats")) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            } else if (args[0].equalsIgnoreCase("logs")) {
                completions.addAll(Arrays.asList("jail", "unjail"));
                jailManager.getAllJailedPlayers().forEach(data -> completions.add(data.getPlayerName()));
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("time")) {
            completions.addAll(Arrays.asList("add", "remove", "set"));
        } else if (args.length == 4 && args[0].equalsIgnoreCase("time")) {
            completions.addAll(Arrays.asList("30m", "1h", "2h", "1d", "permanent"));
        }

        return completions;
    }
}