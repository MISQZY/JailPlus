package org.misqzy.jailPlus.listeners;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.misqzy.jailPlus.data.PlayerJailData;
import org.misqzy.jailPlus.managers.ConfigManager;
import org.misqzy.jailPlus.managers.JailManager;
import org.misqzy.jailPlus.managers.LocalizationManager;
import org.misqzy.jailPlus.utils.TimeUtils;

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

            // Teleport to jail
            String jailName = jailData.getJailName();
            if (jailManager.getJail(jailName) != null) {
                player.teleport(jailManager.getJail(jailName).getLocation());
            }

            localizationManager.sendMessage(player, "jail.login-message",
                    jailData.getJailName(),
                    TimeUtils.formatTime(jailData.getRemainingTime()),
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

        // Bypass for players with permission
        if (player.hasPermission("jailplus.bypass")) {
            return;
        }

        String command = event.getMessage().toLowerCase();
        String[] parts = command.split(" ");
        String commandName = parts[0].substring(1);

        // Check unblocked commands
        for (String allowed : configManager.getUnblockedCommands()) {
            if (commandName.equals(allowed) || commandName.startsWith(allowed + ":")) {
                return; // Allow this command
            }
        }

        // Block the command
        event.setCancelled(true);
        localizationManager.sendMessage(player, "jail.command-blocked");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!configManager.isPreventTeleport()) {
            return;
        }

        Player player = event.getPlayer();

        if (!jailManager.isPlayerJailed(player)) {
            return;
        }

        // Bypass for players with permission
        if (player.hasPermission("jailplus.bypass")) {
            return;
        }

        // Allow teleport by plugin (for unjailing, etc.)
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            return;
        }

        event.setCancelled(true);
        localizationManager.sendMessage(player, "jail.teleport-blocked");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!configManager.isPreventBlockBreak()) {
            return;
        }

        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player) && !player.hasPermission("jailplus.bypass")) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.action-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.isPreventBlockPlace()) {
            return;
        }

        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player) && !player.hasPermission("jailplus.bypass")) {
            event.setCancelled(true);
            localizationManager.sendMessage(player, "jail.action-blocked");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!configManager.isPreventPvP()) {
            return;
        }

        if (event.getDamager() instanceof Player attacker) {
            if (jailManager.isPlayerJailed(attacker) && !attacker.hasPermission("jailplus.bypass")) {
                event.setCancelled(true);
                localizationManager.sendMessage(attacker, "jail.pvp-blocked");
                return;
            }
        }

        if (event.getEntity() instanceof Player victim) {
            if (jailManager.isPlayerJailed(victim) && event.getDamager() instanceof Player attacker) {
                if (!jailManager.isPlayerJailed(attacker) && configManager.isPreventDamageToPrisoners()) {
                    event.setCancelled(true);
                    localizationManager.sendMessage(attacker, "jail.cannot-attack-prisoner");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!configManager.isPreventInventory()) {
            return;
        }

        if (event.getPlayer() instanceof Player player) {
            if (jailManager.isPlayerJailed(player) && !player.hasPermission("jailplus.bypass")) {
                if (!event.getInventory().equals(player.getInventory())) {
                    event.setCancelled(true);
                    localizationManager.sendMessage(player, "jail.inventory-blocked");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (jailManager.isPlayerJailed(player)) {
            PlayerJailData jailData = jailManager.getJailData(player);
            if (jailData != null) {
                // Add prisoner prefix to chat
                String prefix = localizationManager.getRawMessage("jail.chat-prefix");
                event.setFormat(prefix + " " + event.getFormat());
            }
        }
    }
}