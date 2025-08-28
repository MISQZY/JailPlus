package org.misqzy.jailPlus.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.managers.ConfigManager;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;

public class PlayerListener implements Listener {

    private final JailManager jailManager;
    private final LocalizationManager localizationManager;
    private final ConfigManager configManager;

    public PlayerListener(JailManager jailManager, LocalizationManager localizationManager, ConfigManager configManager) {
        this.jailManager = jailManager;
        this.localizationManager = localizationManager;
        this.configManager = configManager;
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            PlayerJailData jailData = jailManager.getJailData(player);

            if (jailData.isExpired()) {
                jailManager.unjailPlayer(player.getUniqueId());
                return;
            }


            String jailName = jailData.getJailName();
            if (jailManager.getJail(jailName) != null) {
                player.teleport(jailManager.getJail(jailName).getLocation());
            }

            localizationManager.sendMessage(player, "jail.login-message",
                    jailData.getJailName(),
                    formatTime(jailData.getRemainingTime()),
                    jailData.getReason()
            );
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            jailManager.savePlayers();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            PlayerJailData jailData = jailManager.getJailData(player);

            if (jailData.isExpired()) {
                jailManager.unjailPlayer(player.getUniqueId());
                return;
            }

            String jailName = jailData.getJailName();
            if (jailManager.getJail(jailName) != null) {
                event.setRespawnLocation(jailManager.getJail(jailName).getLocation());
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!configManager.isPreventCommandUsage()) {
            return;
        }

        Player player = event.getPlayer();

        if (!jailManager.isPlayerJailed(player)) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        String[] parts = command.split(" ");
        String commandName = parts[0].substring(1);

        String[] unblockedCommands = JailPlus.getInstance().getConfigManager().getBlockedCommands().toArray(String[]::new);

        boolean isBlocked = true;
        for (String allowed : unblockedCommands) {
            if (commandName.equals(allowed) || commandName.startsWith(allowed + ":")) {
                isBlocked = false;
                break;
            }
        }

        if (isBlocked) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.command-blocked");
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (!jailManager.isPlayerJailed(player)) {
            return;
        }

        PlayerJailData jailData = jailManager.getJailData(player);
        if (jailData != null) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.teleport-blocked");
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.action-blocked");
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.action-blocked");
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {

            if (jailManager.isPlayerJailed(attacker)) {
                event.setCancelled(true);
                localizationManager.sendMessage(attacker, "jail.pvp-blocked");
            }
        }

        if (event.getEntity() instanceof Player victim) {

            if (jailManager.isPlayerJailed(victim) && event.getDamager() instanceof Player attacker) {
                if (!jailManager.isPlayerJailed(attacker)) {
                    event.setCancelled(true);
                    localizationManager.sendMessage(attacker, "jail.cannot-attack-prisoner");
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {

            if (jailManager.isPlayerJailed(player)) {
                if (!event.getInventory().equals(player.getInventory())) {
                    event.setCancelled(true);
                    localizationManager.sendMessage(player, "jail.inventory-blocked");
                }
            }
        }
    }

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " s";
        } else if (seconds < 3600) {
            return (seconds / 60) + " m";
        } else if (seconds < 86400) {
            return (seconds / 3600) + " h";
        } else {
            return (seconds / 86400) + " d";
        }
    }
}
