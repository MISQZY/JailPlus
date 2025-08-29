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
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;
import org.misqzy.jailPlus.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JailCommand implements CommandExecutor, TabCompleter {

    private final JailPlus plugin;
    private final JailManager jailManager;
    private final LocalizationManager localizationManager;

    public JailCommand(JailPlus plugin, JailManager jailManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
        this.localizationManager = localizationManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                showHelp(sender);
                break;

            case "list":
                handleListCommand(sender, args);
                break;

            case "info":
                handleInfoCommand(sender, args);
                break;

            case "player":
                handleJailCommand(sender, args);
                break;

            default:
                localizationManager.sendMessage(sender, "no-permission");
                break;
        }

        return true;
    }

    private void handleJailCommand(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.jail.player"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (args.length < 2) {
            localizationManager.sendMessage(sender, "jail.usage");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            localizationManager.sendMessage(sender, "player.not-found", targetName);
            return;
        }

        if (jailManager.isPlayerJailed(target)) {
            localizationManager.sendMessage(sender, "jail.already-jailed", target.getName());
            return;
        }

        String jailName = null;
        long jailTime = 1800; // Default 30 minutes
        String reason = "No reason";

        int index = 2;

        // Try to find jail name
        if (args.length > index) {
            String possibleJailName = args[index];
            if (jailManager.getJail(possibleJailName) != null) {
                jailName = possibleJailName;
                index++;
            }
        }

        // Use default jail if none specified
        if (jailName == null) {
            jailName = jailManager.getAllJails().stream()
                    .map(JailData::getName)
                    .findFirst()
                    .orElse(null);
        }

        if (jailName == null) {
            localizationManager.sendMessage(sender, "jail.no-available-jails");
            return;
        }

        // Parse time
        if (args.length > index) {
            try {
                jailTime = TimeUtils.parseTime(args[index]);
                index++;

                if (jailTime <= 0) {
                    localizationManager.sendMessage(sender, "jail.invalid-time");
                    return;
                }

                int maxTime = plugin.getConfigManager().getMaxJailTime();
                if (jailTime > maxTime && maxTime > 0) {
                    localizationManager.sendMessage(sender, "jail.time-too-long", TimeUtils.formatTime(maxTime));
                    return;
                }
            } catch (IllegalArgumentException e) {
                localizationManager.sendMessage(sender, "jail.invalid-time-format");
                return;
            }
        }

        // Parse reason
        if (args.length > index) {
            reason = String.join(" ", Arrays.copyOfRange(args, index, args.length));
        }

        String jailedBy = sender instanceof Player ? sender.getName() : "Console";
        boolean success = jailManager.jailPlayer(target, jailName, jailTime, reason, jailedBy);

        if (success) {
            localizationManager.sendMessage(sender, "jail.success",
                    target.getName(), TimeUtils.formatTime(jailTime), reason);
        } else {
            localizationManager.sendMessage(sender, "jail.failed", target.getName());
        }
    }

    private void handleListCommand(CommandSender sender, String[] args) {
        if ((sender instanceof Player player) && (!sender.hasPermission("jailplus.jail.list"))) {
            localizationManager.sendMessage(player, "no-permission");
            return;
        }

        if (jailManager.getAllJailedPlayers().isEmpty()) {
            localizationManager.sendMessage(sender, "jail.list-empty");
            return;
        }

        localizationManager.sendMessage(sender, "jail.list-header");

        for (PlayerJailData jailData : jailManager.getAllJailedPlayers()) {
            long remainingTime = jailData.getRemainingTime();
            localizationManager.sendMessage(sender, "jail.list-entry",
                    jailData.getPlayerName(),
                    jailData.getJailName(),
                    TimeUtils.formatTime(remainingTime),
                    jailData.getReason()
            );
        }
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("jailplus.jail.info") && sender instanceof Player) {
            localizationManager.sendMessage(sender, "no-permission");
            return;
        }

        Player targetPlayer;
        String targetName;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                localizationManager.sendMessage(sender, "no-permission");
                return;
            }

            targetPlayer = (Player) sender;
            targetName = sender.getName();
        } else {
            targetName = args[1];
            targetPlayer = Bukkit.getPlayer(targetName);

            if (targetPlayer == null) {
                localizationManager.sendMessage(sender, "player.not-found", targetName);
                return;
            }
        }

        PlayerJailData jailData = jailManager.getJailData(targetPlayer);
        if (jailData == null) {
            if (args.length < 2) {
                localizationManager.sendMessage(sender, "jail.you-not-jailed");
            } else {
                localizationManager.sendMessage(sender, "jail.not-jailed", targetPlayer.getName());
            }
            return;
        }

        long remainingTime = jailData.getRemainingTime();
        String timeFormatted = TimeUtils.formatTime(remainingTime);

        if (args.length < 2) {
            if (sender.hasPermission("jailplus.jail.info.own")) {
                localizationManager.sendMessage(sender, "jail.your-info",
                        jailData.getJailName(),
                        timeFormatted,
                        jailData.getReason(),
                        jailData.getJailedBy()
                );
            } else {
                localizationManager.sendMessage(sender, "no-permission");
            }
        } else {
            if (sender instanceof Player) {
                if (sender.hasPermission("jailplus.jail.info.other")) {
                    localizationManager.sendMessage(sender, "jail.info",
                            jailData.getPlayerName(),
                            jailData.getJailName(),
                            timeFormatted,
                            jailData.getReason(),
                            jailData.getJailedBy()
                    );
                } else {
                    localizationManager.sendMessage(sender, "no-permission");
                }
            } else {
                localizationManager.sendMessage(sender, "jail.info",
                        jailData.getPlayerName(),
                        jailData.getJailName(),
                        timeFormatted,
                        jailData.getReason(),
                        jailData.getJailedBy()
                );
            }
        }
    }

    private void showHelp(CommandSender sender) {
        localizationManager.sendMessage(sender, "jail.help-header");
        localizationManager.sendMessage(sender, "jail.help-jail");
        localizationManager.sendMessage(sender, "jail.help-list");
        localizationManager.sendMessage(sender, "jail.help-info");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "player", "list", "info"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("player")) {
                Bukkit.getOnlinePlayers().forEach(player -> completions.add(player.getName()));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("player")) {
                List<String> jailNames = jailManager.getAllJails().stream()
                        .map(JailData::getName)
                        .toList();
                completions.addAll(jailNames);
                completions.addAll(Arrays.asList("30m", "1h", "2h", "1d", "permanent"));
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("player")) {
                completions.addAll(Arrays.asList("30m", "1h", "2h", "1d", "permanent"));
            }
        }

        return completions;
    }
}
