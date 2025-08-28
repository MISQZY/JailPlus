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
import org.misqzy.jailPlus.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JailAdminCommand implements CommandExecutor, TabCompleter {

    private final JailPlus plugin;
    private final JailManager jailManager;
    private final LocalizationManager localizationManager;

    public JailAdminCommand(JailPlus plugin, JailManager jailManager, ConfigManager configManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
        this.localizationManager = localizationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command,@NotNull String label, String[] args) {
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
            localizationManager.sendMessage( sender, "no-permission");
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
        if ((sender instanceof Player player) &&
                (!sender.hasPermission("jailplus.admin.delete"))) {
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
            localizationManager.sendMessage( sender, "admin.jail-deleted", jailName);
        } else {
            localizationManager.sendMessage( sender, "admin.jail-delete-failed", jailName);
        }
    }

    private void handleListJails(CommandSender sender) {
        if ((sender instanceof Player player)
                && (!sender.hasPermission("jailplus.admin.list"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (jailManager.getAllJails().isEmpty()) {
            localizationManager.sendMessage(sender, "admin.no-jails");
            return;
        }

        localizationManager.sendMessage(sender, "admin.jails-header");

        for (JailData jail : jailManager.getAllJails()) {
            localizationManager.sendMessage(sender, "admin.jail-entry",
                    jail.getName(),
                    jail.getWorldName(),
                    (int) jail.getX(),
                    (int) jail.getY(),
                    (int) jail.getZ()
            );
        }
    }


    private void handleJailInfo(CommandSender sender, String[] args) {
        if ((sender instanceof Player player)
                && (!sender.hasPermission("jailplus.admin.info"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }
        if (args.length < 2) {
            localizationManager.sendMessage( sender, "admin.info-usage");
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
        if ((sender instanceof Player player)
                && (!sender.hasPermission("jailplus.admin.time"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 4) {
            localizationManager.sendMessage( sender, "jail.time-usage");
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

            switch (action) {
                case "add":
                    jailData.addTime(time);
                    localizationManager.sendMessage(sender, "jail.time-added",
                            target.getName(), TimeUtils.formatTime(time));
                    break;

                case "remove":
                case "subtract":
                    jailData.subtractTime(time);
                    localizationManager.sendMessage(sender, "jail.time-removed",
                            target.getName(), TimeUtils.formatTime(time));
                    break;

                case "set":
                    jailData.setTime(time);
                    localizationManager.sendMessage(sender, "jail.time-set",
                            target.getName(), TimeUtils.formatTime(time));
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
        if ((sender instanceof Player player)
                && (!sender.hasPermission("jailplus.reload"))) {
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


    private void showHelp(CommandSender sender) {
        localizationManager.sendMessage(sender, "admin.help-header");
        localizationManager.sendMessage(sender, "admin.help-create");
        localizationManager.sendMessage(sender, "admin.help-delete");
        localizationManager.sendMessage(sender, "admin.help-list");
        localizationManager.sendMessage(sender, "admin.help-info");
        localizationManager.sendMessage(sender, "admin.help-reload");
        localizationManager.sendMessage(sender, "admin.help-time");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "delete", "list", "info", "reload", "help", "time"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("delete")
                    || args[0].equalsIgnoreCase("info")
                    ) {
                jailManager.getAllJails().forEach(jail -> completions.add(jail.getName()));
            }
            else if (args[0].equalsIgnoreCase("time")) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            }
        }
        else if (args.length == 3 && args[0].equalsIgnoreCase("time")) {
            completions.addAll(Arrays.asList("add", "remove", "set"));
        }
        else if (args.length == 4 && args[0].equalsIgnoreCase("time")) {
            completions.addAll(Arrays.asList("30m", "1h", "2h", "1d", "permanent"));
        }

        return completions;
    }
}
