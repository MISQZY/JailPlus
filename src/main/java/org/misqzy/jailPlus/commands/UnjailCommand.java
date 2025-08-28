package org.misqzy.jailPlus.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;

import java.util.ArrayList;
import java.util.List;

public class UnjailCommand implements CommandExecutor, TabCompleter {

    private final JailPlus plugin;
    private final JailManager jailManager;
    private final LocalizationManager localizationManager;

    public UnjailCommand(JailPlus plugin, JailManager jailManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.jailManager = jailManager;
        this.localizationManager = localizationManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command,@NotNull String label, String[] args) {
        if (!sender.hasPermission("jailplus.unjail")
                && (sender instanceof Player)) {
                localizationManager.sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length != 1) {
            localizationManager.sendMessage(sender, "unjail.usage");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target != null) {
            if (!jailManager.isPlayerJailed(target)) {
                localizationManager.sendMessage(sender, "jail.not-jailed", target.getName());
                return true;
            }

            boolean success = jailManager.unjailPlayer(target.getUniqueId());
            if (success) {
                localizationManager.sendMessage(sender, "unjail.success", target.getName());
            } else {
                localizationManager.sendMessage(sender, "unjail.failed", target.getName());
            }
        } else {
            boolean success = jailManager.unjailPlayer(targetName);
            if (success) {
                localizationManager.sendMessage(sender, "unjail.success", targetName);
            } else {
                localizationManager.sendMessage(sender, "unjail.not-found", targetName);
            }
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,@NotNull Command command,@NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            jailManager.getAllJailedPlayers().forEach(jailData ->
                    completions.add(jailData.getPlayerName())
            );
        }

        return completions;
    }
}
