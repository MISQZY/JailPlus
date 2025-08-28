package org.misqzy.jailPlus.managers;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.misqzy.jailPlus.JailPlus;
import org.misqzy.jailPlus.data.JailData;
import org.misqzy.jailPlus.data.PlayerJailData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class JailManager {

    private final JailPlus plugin;
    private final ConfigManager configManager;
    private final LocalizationManager localizationManager;


    private final Map<String, JailData> jails;
    private final Map<UUID, PlayerJailData> jailedPlayers;


    private File jailsFile;
    private File playersFile;
    private FileConfiguration jailsConfig;
    private FileConfiguration playersConfig;


    private BukkitRunnable jailTimer;

    public JailManager(JailPlus plugin, ConfigManager configManager, LocalizationManager localizationManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.localizationManager = localizationManager;
        this.jails = new ConcurrentHashMap<>();
        this.jailedPlayers = new ConcurrentHashMap<>();

        setupDataFiles();
        loadData();
        startJailTimer();
    }


    private void setupDataFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        jailsFile = new File(dataFolder, "jails.yml");
        playersFile = new File(dataFolder, "players.yml");

        if (!jailsFile.exists()) {
            try {
                jailsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error when try to create jails.yml", e);
            }
        }

        if (!playersFile.exists()) {
            try {
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Error when try to create players.yml", e);
            }
        }

        jailsConfig = YamlConfiguration.loadConfiguration(jailsFile);
        playersConfig = YamlConfiguration.loadConfiguration(playersFile);
    }


    private void loadData() {
        loadJails();
        loadJailedPlayers();
    }


    private void loadJails() {
        jails.clear();

        if (jailsConfig.getConfigurationSection("jails") == null) {
            return;
        }

        for (String jailName : jailsConfig.getConfigurationSection("jails").getKeys(false)) {
            String path = "jails." + jailName;

            String worldName = jailsConfig.getString(path + ".world");
            double x = jailsConfig.getDouble(path + ".x");
            double y = jailsConfig.getDouble(path + ".y");
            double z = jailsConfig.getDouble(path + ".z");
            float yaw = (float) jailsConfig.getDouble(path + ".yaw");
            float pitch = (float) jailsConfig.getDouble(path + ".pitch");

            JailData jail = new JailData(jailName, worldName, x, y, z, yaw, pitch);
            jails.put(jailName, jail);
        }

        plugin.getLogger().info("Loaded  " + jails.size() + " jails");
    }


    private void loadJailedPlayers() {
        jailedPlayers.clear();

        if (playersConfig.getConfigurationSection("players") == null) {
            return;
        }

        for (String uuidString : playersConfig.getConfigurationSection("players").getKeys(false)) {
            String path = "players." + uuidString;

            UUID uuid = UUID.fromString(uuidString);
            String playerName = playersConfig.getString(path + ".name");
            String jailName = playersConfig.getString(path + ".jail");
            long jailTime = playersConfig.getLong(path + ".jail-time");
            long startTime = playersConfig.getLong(path + ".start-time");
            String reason = playersConfig.getString(path + ".reason", "No reason");
            String jailedBy = playersConfig.getString(path + ".jailed-by", "Console");

            PlayerJailData jailData = new PlayerJailData(uuid, playerName, jailName, jailTime, reason, jailedBy);

            String startTimePath = path + ".start-time";
            if (playersConfig.contains(startTimePath)) {
                long currentTime = System.currentTimeMillis() / 1000;
                long elapsedTime = currentTime - startTime;
                long remainingTime = Math.max(0, jailTime - elapsedTime);

                if (remainingTime > 0) {
                    jailData.setTime(remainingTime);
                }
            }


            if (playersConfig.contains(path + ".previous-location")) {
                String prevPath = path + ".previous-location";
                String worldName = playersConfig.getString(prevPath + ".world");
                double x = playersConfig.getDouble(prevPath + ".x");
                double y = playersConfig.getDouble(prevPath + ".y");
                double z = playersConfig.getDouble(prevPath + ".z");
                float yaw = (float) playersConfig.getDouble(prevPath + ".yaw");
                float pitch = (float) playersConfig.getDouble(prevPath + ".pitch");

                if (worldName != null && Bukkit.getWorld(worldName) != null) {
                    Location prevLocation = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                    jailData.setPreviousLocation(prevLocation);
                }
            }

            jailedPlayers.put(uuid, jailData);
        }

        plugin.getLogger().info("Loaded  " + jailedPlayers.size() + " prisoners");
    }


    public boolean createJail(String name, Location location) {
        if (jails.containsKey(name.toLowerCase())) {
            return false;
        }

        JailData jail = new JailData(name.toLowerCase(), location);
        jails.put(name.toLowerCase(), jail);
        saveJails();
        return true;
    }


    public boolean deleteJail(String name) {
        String jailName = name.toLowerCase();
        if (!jails.containsKey(jailName)) {
            return false;
        }


        jailedPlayers.values().stream()
                .filter(data -> data.getJailName().equals(jailName))
                .map(PlayerJailData::getPlayerUuid)
                .forEach(this::unjailPlayer);

        jails.remove(jailName);
        saveJails();
        return true;
    }


    public boolean jailPlayer(Player player, String jailName, long time, String reason, String jailedBy) {
        if (isPlayerJailed(player)) {
            return false;
        }

        JailData jail = jails.get(jailName.toLowerCase());
        if (jail == null) {
            return false;
        }


        Location currentLocation = player.getLocation();


        PlayerJailData jailData = new PlayerJailData(
                player.getUniqueId(),
                player.getName(),
                jailName.toLowerCase(),
                time,
                reason,
                jailedBy
        );
        jailData.setPreviousLocation(currentLocation);


        Location jailLocation = jail.getLocation();
        if (jailLocation != null) {
            player.teleport(jailLocation);
        }


        jailedPlayers.put(player.getUniqueId(), jailData);

        savePlayers();

        if (configManager.isBroadcastJail()) {
            Component comp = localizationManager.getMessage("jail.broadcast", player.getName(), reason, formatTime(time));
            Bukkit.broadcast(comp);
        }

        localizationManager.sendMessage(player, "jail.jailed",
                jailName, formatTime(time), reason);

        return true;
    }


    public boolean unjailPlayer(UUID playerUuid) {
        PlayerJailData jailData = jailedPlayers.remove(playerUuid);
        if (jailData == null) {
            return false;
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null) {
            Location returnLocation = jailData.getPreviousLocation();
            if (returnLocation != null) {
                player.teleport(returnLocation);
            } else {
                player.teleport(player.getWorld().getSpawnLocation());
            }


            localizationManager.sendMessage(player, "jail.unjailed");


            if (configManager.isBroadcastUnjail()) {
                Component comp = localizationManager.getMessage("jail.unjail-broadcast", player.getName());
                Bukkit.broadcast(comp);
            }
        }

        savePlayers();
        return true;
    }


    public boolean unjailPlayer(String playerName) {
        UUID uuid = jailedPlayers.values().stream()
                .filter(data -> data.getPlayerName().equalsIgnoreCase(playerName))
                .map(PlayerJailData::getPlayerUuid)
                .findFirst()
                .orElse(null);

        return uuid != null && unjailPlayer(uuid);
    }


    public boolean isPlayerJailed(Player player) {
        return jailedPlayers.containsKey(player.getUniqueId());
    }


    public PlayerJailData getJailData(Player player) {
        return jailedPlayers.get(player.getUniqueId());
    }


    public JailData getJail(String name) {
        return jails.get(name.toLowerCase());
    }


    public Collection<JailData> getAllJails() {
        return jails.values();
    }


    public Collection<PlayerJailData> getAllJailedPlayers() {
        return jailedPlayers.values();
    }


    private void startJailTimer() {
        jailTimer = new BukkitRunnable() {
            @Override
            public void run() {
                checkJailTimes();
            }
        };

        jailTimer.runTaskTimer(plugin, 200L, 200L);
    }


    private void checkJailTimes() {
        List<UUID> toRelease = new ArrayList<>();

        for (PlayerJailData jailData : jailedPlayers.values()) {
            if (jailData.isExpired()) {
                toRelease.add(jailData.getPlayerUuid());
            }
        }

        for (UUID uuid : toRelease) {
            unjailPlayer(uuid);
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


    public void saveJails() {
        jailsConfig.set("jails", null);

        for (JailData jail : jails.values()) {
            String path = "jails." + jail.getName();
            jailsConfig.set(path + ".world", jail.getWorldName());
            jailsConfig.set(path + ".x", jail.getX());
            jailsConfig.set(path + ".y", jail.getY());
            jailsConfig.set(path + ".z", jail.getZ());
            jailsConfig.set(path + ".yaw", jail.getYaw());
            jailsConfig.set(path + ".pitch", jail.getPitch());
        }

        try {
            jailsConfig.save(jailsFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error when try to save jails.yml", e);
        }
    }


    public void savePlayers() {
        playersConfig.set("players", null);

        for (PlayerJailData jailData : jailedPlayers.values()) {
            String path = "players." + jailData.getPlayerUuid().toString();
            playersConfig.set(path + ".name", jailData.getPlayerName());
            playersConfig.set(path + ".jail", jailData.getJailName());
            playersConfig.set(path + ".jail-time", jailData.getJailTime());
            playersConfig.set(path + ".start-time", jailData.getStartTime());
            playersConfig.set(path + ".reason", jailData.getReason());
            playersConfig.set(path + ".jailed-by", jailData.getJailedBy());

            Location prevLocation = jailData.getPreviousLocation();
            if (prevLocation != null) {
                String prevPath = path + ".previous-location";
                playersConfig.set(prevPath + ".world", prevLocation.getWorld().getName());
                playersConfig.set(prevPath + ".x", prevLocation.getX());
                playersConfig.set(prevPath + ".y", prevLocation.getY());
                playersConfig.set(prevPath + ".z", prevLocation.getZ());
                playersConfig.set(prevPath + ".yaw", prevLocation.getYaw());
                playersConfig.set(prevPath + ".pitch", prevLocation.getPitch());
            }
        }

        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error when try to save players.yml", e);
        }
    }


    public void saveAllData() {
        saveJails();
        savePlayers();
    }


    public void reloadData() {
        if (jailTimer != null) {
            jailTimer.cancel();
        }

        loadData();
        startJailTimer();
        plugin.getLogger().info("JailPlus data reloaded!");
    }


    public void shutdown() {
        if (jailTimer != null) {
            jailTimer.cancel();
        }
        saveAllData();
    }
}
